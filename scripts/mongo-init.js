// MongoDB Initialization Script

// Switch to event_processing database
db = db.getSiblingDB('event_processing');

// Create collections with validation
db.createCollection('events', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['eventId', 'eventType', 'timestamp', 'payload'],
            properties: {
                eventId: {
                    bsonType: 'string',
                    description: 'Unique event identifier - required'
                },
                eventType: {
                    bsonType: 'string',
                    description: 'Type of event - required'
                },
                timestamp: {
                    bsonType: 'date',
                    description: 'Event timestamp - required'
                },
                payload: {
                    bsonType: 'object',
                    description: 'Event payload - required'
                },
                status: {
                    enum: ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'],
                    description: 'Event processing status'
                },
                retryCount: {
                    bsonType: 'int',
                    minimum: 0,
                    description: 'Number of retry attempts'
                }
            }
        }
    }
});

// Create indexes for performance
db.events.createIndex({ eventId: 1 }, { unique: true });
db.events.createIndex({ eventType: 1, timestamp: -1 });
db.events.createIndex({ status: 1, timestamp: -1 });
db.events.createIndex({ timestamp: -1 });

// Create collection for failed events (Dead Letter Queue)
db.createCollection('failed_events');
db.failed_events.createIndex({ eventId: 1 });
db.failed_events.createIndex({ originalTimestamp: -1 });
db.failed_events.createIndex({ failureReason: 1 });

// Create collection for metrics
db.createCollection('metrics');
db.metrics.createIndex({ metricName: 1, timestamp: -1 });
db.metrics.createIndex({ timestamp: -1 });

// Create collection for alerts
db.createCollection('alerts');
db.alerts.createIndex({ alertType: 1, timestamp: -1 });
db.alerts.createIndex({ resolved: 1, timestamp: -1 });

print('MongoDB initialization completed successfully!');
