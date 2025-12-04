# Real-Time Event Processing Platform

A high-performance, serverless event delivery pipeline built with Java, Spring Boot, Apache Kafka, LocalStack, and MongoDB. Designed to handle 100K+ events per second with sub-second latency and 99.9% delivery success rate.

## 🏗️ Architecture

This platform implements a distributed event processing system with:

- **Event Producer**: REST API for event submission with Kafka and SQS publishing
- **Event Consumer**: Kafka consumer with circuit breaker, retry logic, and self-healing
- **Data Ingestion**: MongoDB persistence layer with optimized indexing
- **Monitoring**: CloudWatch metrics, alerting, and observability

## 🚀 Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Message Queue**: Apache Kafka 3.6
- **Cloud Services**: LocalStack (SQS, Lambda, SNS, CloudWatch)
- **Database**: MongoDB 7.0
- **Build Tool**: Gradle 8.x
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Gradle 8.x (or use the wrapper)
- AWS CLI (for LocalStack setup)
- 8GB+ RAM recommended

## 🎯 Quick Start

### 1. Start Infrastructure Services

```bash
# Start LocalStack, Kafka, and MongoDB
docker-compose up -d

# Wait for services to be ready
docker-compose ps
```

### 2. Initialize LocalStack Resources

```powershell
# Windows (PowerShell)
.\scripts\setup-localstack.ps1

# Linux/Mac
chmod +x scripts/setup-localstack.sh
./scripts/setup-localstack.sh
```

### 3. Build All Services

```bash
# Build all modules
./gradlew build

# Skip tests for faster build
./gradlew build -x test
```

### 4. Run Services

```bash
# Terminal 1: Event Producer
./gradlew :event-producer:bootRun

# Terminal 2: Event Consumer
./gradlew :event-consumer:bootRun

# Terminal 3: Data Ingestion Service
./gradlew :data-ingestion-service:bootRun

# Terminal 4: Monitoring Service
./gradlew :monitoring-service:bootRun
```

### 5. Submit Test Events

```bash
# Submit a single event
curl -X POST http://localhost:8081/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "user.created",
    "payload": {
      "userId": "123",
      "username": "john_doe",
      "email": "john@example.com"
    },
    "metadata": {
      "priority": "high"
    }
  }'
```

## 📊 Monitoring & Observability

### Service Endpoints

- **Event Producer**: http://localhost:8081
- **Event Consumer**: http://localhost:8082
- **Data Ingestion**: http://localhost:8083
- **Monitoring Service**: http://localhost:8084

### Monitoring Dashboards

- **Kafka UI**: http://localhost:8080
- **MongoDB Express**: http://localhost:8081 (admin/admin123)
- **Prometheus Metrics**: 
  - Producer: http://localhost:8081/actuator/prometheus
  - Consumer: http://localhost:8082/actuator/prometheus
  - Data Ingestion: http://localhost:8083/actuator/prometheus
  - Monitoring: http://localhost:8084/actuator/prometheus

### Health Checks

```bash
# Check all services
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

## 📦 Project Structure

```
Real-Time-Event-Processing/
├── common/                      # Shared models, utilities, and configurations
├── event-producer/              # Event submission and publishing service
├── event-consumer/              # Event consumption and processing service
├── data-ingestion-service/      # MongoDB persistence layer
├── monitoring-service/          # Metrics and alerting service
├── scripts/                     # Setup and utility scripts
├── docker-compose.yml           # Infrastructure services
├── build.gradle                 # Root build configuration
└── settings.gradle              # Multi-module configuration
```

## 🔧 Configuration

### Environment Variables

Key configuration can be overridden via environment variables:

```bash
# Kafka
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092

# MongoDB
export SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@localhost:27017/event_processing?authSource=admin

# AWS/LocalStack
export AWS_ENDPOINT=http://localhost:4566
export AWS_REGION=us-east-1
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :event-consumer:test

# Run with coverage
./gradlew test jacocoTestReport
```

## 📈 Performance

Target metrics:

- **Throughput**: 100,000+ events/second
- **Latency**: Sub-second (P99 < 1000ms)
- **Delivery Success**: 99.9%
- **Availability**: 99.99%

## 🛠️ Troubleshooting

### Issue: Services can't connect to Kafka

```bash
# Check Kafka is running
docker-compose ps kafka

# View Kafka logs
docker-compose logs kafka
```

### Issue: MongoDB connection failed

```bash
# Verify MongoDB is running
docker-compose ps mongodb

# Test connection
mongosh mongodb://admin:admin123@localhost:27017
```

### Issue: LocalStack services not accessible

```bash
# Restart LocalStack
docker-compose restart localstack

# Re-run setup script
.\scripts\setup-localstack.ps1
```

## 📚 Additional Documentation

- [Architecture Details](ARCHITECTURE.md)
- [Operational Runbook](RUNBOOK.md)
- [API Documentation](API.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 👤 Author

**Ankit Verma**

---

For more information or support, please refer to the [RUNBOOK.md](RUNBOOK.md) for operational procedures.
