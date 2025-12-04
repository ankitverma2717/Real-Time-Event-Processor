# Operational Runbook

This runbook provides operational procedures for the Real-Time Event Processing Platform, including monitoring, troubleshooting, and incident response.

## 🎯 Service Level Objectives (SLOs)

| Metric | Target | Alert Threshold |
|--------|--------|----------------|
| Availability | 99.99% | < 99.9% |
| Throughput | 100K events/sec | < 50K events/sec |
| Latency (P99) | < 1000ms | > 2000ms |
| Error Rate | < 0.1% | > 5% |
| Delivery Success | 99.9% | < 99% |

## 📋 Pre-Deployment Checklist

- [ ] All services pass health checks
- [ ] LocalStack services are running
- [ ] Kafka cluster is healthy
- [ ] MongoDB is accessible
- [ ] All required topics/queues exist
- [ ] Metrics dashboards are configured
- [ ] Alert notifications are set up

## 🔍 Monitoring

### Key Metrics to Watch

#### System Health
```bash
# Check all service health
for port in 8081 8082 8083 8084; do
  echo "Checking service on port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

#### Kafka Metrics
- Consumer lag (should be < 1000)
- Partition distribution
- Rebalance frequency

#### MongoDB Metrics
- Connection pool utilization
- Query execution time
- Index usage

#### CloudWatch Metrics
- EventThroughput
- EventLatency
- ErrorRate
- FailedEvents count

### Accessing Logs

```bash
# Service logs (if running via Gradle)
./gradlew :event-producer:bootRun

# Docker container logs
docker-compose logs -f kafka
docker-compose logs -f mongodb
docker-compose logs -f localstack

# MongoDB query logs
docker-compose exec mongodb mongosh event_processing --eval "db.setProfilingLevel(1, 100)"
```

## 🚨 Common Issues and Solutions

### Issue 1: High Consumer Lag

**Symptoms:**
- Kafka consumer lag > 10,000
- Events processing slowly
- SQS queue depth increasing

**Diagnosis:**
```bash
# Check consumer group lag
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server kafka:9092 \
  --group event-consumer-group \
  --describe
```

**Resolution:**
1. Scale consumer concurrency in `application.yml`
2. Check for slow event processing (database bottleneck)
3. Verify circuit breaker is not open
4. Check for deadlocks or blocking operations

**Prevention:**
- Monitor consumer lag in real-time
- Set up alerts for lag > 5000
- Implement auto-scaling based on lag

---

### Issue 2: Circuit Breaker Open

**Symptoms:**
- Events going directly to DLQ
- Logs showing "Circuit breaker opened"
- Increased failed event count

**Diagnosis:**
```bash
# Check circuit breaker status
curl http://localhost:8082/actuator/circuitbreakers
curl http://localhost:8082/actuator/circuitbreakerevents
```

**Resolution:**
1. Identify root cause of failures
2. Fix underlying issue (database, external service, etc.)
3. Wait for circuit breaker to transition to HALF_OPEN
4. Monitor success rate as circuit breaker recovers

**Prevention:**
- Set appropriate failure thresholds
- Implement proper retry strategies
- Use bulkheads to isolate failures

---

### Issue 3: MongoDB Performance Degradation

**Symptoms:**
- Slow event ingestion
- High query execution times
- Database connection pool exhaustion

**Diagnosis:**
```bash
# Check slow queries
docker-compose exec mongodb mongosh event_processing --eval "db.system.profile.find().limit(10).sort({ts:-1})"

# Check index usage
docker-compose exec mongodb mongosh event_processing --eval "db.events.getIndexes()"

# Check connection pool stats
curl http://localhost:8083/actuator/metrics/mongodb.driver.pool.size
```

**Resolution:**
1. Add missing indexes for slow queries
2. Optimize batch insertion
3. Increase connection pool size if needed
4. Consider MongoDB sharding for scale

**Prevention:**
- Regular index optimization reviews
- Monitor slow query log
- Implement query performance tests

---

### Issue 4: LocalStack Service Unavailable

**Symptoms:**
- SQS/SNS errors in logs
- CloudWatch metrics not publishing
- Lambda invocation failures

**Diagnosis:**
```bash
# Check LocalStack health
curl http://localhost:4566/_localstack/health

# Verify services are running
aws --endpoint-url=http://localhost:4566 sqs list-queues
aws --endpoint-url=http://localhost:4566 sns list-topics
```

**Resolution:**
1. Restart LocalStack container
2. Re-run setup script to recreate resources
3. Verify environment variables are correct

```bash
docker-compose restart localstack
sleep 10
.\scripts\setup-localstack.ps1
```

**Prevention:**
- Use LocalStack persistence feature
- Monitor LocalStack container health
- Implement retry logic for AWS SDK calls

---

### Issue 5: Event Processing Stuck

**Symptoms:**
- Events in PROCESSING state for extended period
- No events completing
- Consumer appears healthy but not processing

**Diagnosis:**
```bash
# Check stuck events in MongoDB
docker-compose exec mongodb mongosh event_processing --eval "
  db.events.find({
    status: 'PROCESSING',
    timestamp: { \$lt: new Date(Date.now() - 3600000) }
  }).count()
"

# Check consumer threads
curl http://localhost:8082/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total
```

**Resolution:**
1. Identify stuck events
2. Check for deadlocks or infinite loops
3. Manually reset stuck events to PENDING
4. Restart consumer service if necessary

```javascript
// Reset stuck events
db.events.updateMany(
  {
    status: 'PROCESSING',
    timestamp: { $lt: new Date(Date.now() - 3600000) }
  },
  {
    $set: { status: 'PENDING' }
  }
)
```

## 🔄 Routine Maintenance

### Daily Tasks
- [ ] Review error rate and alert status
- [ ] Check consumer lag across all topics
- [ ] Verify all services are healthy
- [ ] Review dead letter queue

### Weekly Tasks
- [ ] Analyze performance trends
- [ ] Review failed events and patterns
- [ ] Check disk usage (MongoDB, Kafka)
- [ ] Update operational documentation

### Monthly Tasks
- [ ] Database index optimization
- [ ] Review and archive old events
- [ ] Capacity planning review
- [ ] Disaster recovery drill

## 📞 Escalation Procedures

### Severity Levels

**P1 - Critical (15 min response)**
- System completely down
- Data loss occurring
- Error rate > 50%

**P2 - High (1 hour response)**
- Significant performance degradation
- Error rate > 10%
- Consumer lag > 100K

**P3 - Medium (4 hour response)**
- Minor performance issues
- Error rate > 5%
- Non-critical alerts

**P4 - Low (Next business day)**
- Questions
- Enhancement requests
- Documentation updates

### On-Call Rotation

For PST operations and cross-timezone support:

1. **Primary On-Call**: Monitors alerts and responds first
2. **Secondary On-Call**: Escalation point if primary unavailable
3. **Escalation to Engineering**: For code-level issues

## 🛡️ Disaster Recovery

### Data Backup

```bash
# Backup MongoDB
docker-compose exec mongodb mongodump --out=/backup/$(date +%Y%m%d)

# Backup Kafka topics (if needed)
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic events --from-beginning > backup.json
```

### Recovery Procedures

1. **Service Failure**: Restart affected service
2. **Data Corruption**: Restore from latest backup
3. **Complete System Failure**: 
   - Redeploy infrastructure
   - Restore MongoDB backup
   - Replay Kafka events if needed

## 📊 Performance Tuning

### Kafka Tuning

```yaml
# Increase partitions for higher throughput
kafka-topics --alter --topic events --partitions 12

# Adjust batch size and linger time
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger-ms=20
```

### MongoDB Tuning

```javascript
// Create additional indexes for common queries
db.events.createIndex({ "metadata.priority": 1, timestamp: -1 })

// Enable compression
use admin
db.runCommand({ setParameter: 1, internalQueryExecMaxBlockingSortBytes: 335544320 })
```

### JVM Tuning

```bash
# Increase heap size for high throughput
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
```

## 🔐 Security

### Credentials Rotation

Monthly rotation of:
- MongoDB admin password
- LocalStack access keys
- Service-to-service tokens

### Access Control

- Monitor unauthorized access attempts
- Review and update IAM policies
- Audit log access regularly

---

**Last Updated**: December 2025  
**Contact**: operations@example.com  
**On-Call Pager**: +1-XXX-XXX-XXXX
