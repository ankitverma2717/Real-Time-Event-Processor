#!/bin/bash

# Setup script for LocalStack AWS resources

echo "Setting up LocalStack resources..."

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
sleep 10

# Set LocalStack endpoint
ENDPOINT="http://localhost:4566"

# Create SQS Queues
echo "Creating SQS queues..."
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name event-processing-queue --region us-east-1
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name event-dlq --region us-east-1
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name high-priority-queue --region us-east-1

# Create SNS Topics
echo "Creating SNS topics..."
aws --endpoint-url=$ENDPOINT sns create-topic --name event-notifications --region us-east-1
aws --endpoint-url=$ENDPOINT sns create-topic --name alert-notifications --region us-east-1

# Create CloudWatch Log Groups
echo "Creating CloudWatch log groups..."
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /aws/lambda/event-processor --region us-east-1
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /events/processing --region us-east-1
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /events/errors --region us-east-1

# Create CloudWatch Alarms (Metrics)
echo "Creating CloudWatch metrics..."
# Note: Metrics will be published by the application

# Subscribe SQS to SNS
echo "Subscribing SQS to SNS topics..."
QUEUE_ARN=$(aws --endpoint-url=$ENDPOINT sqs get-queue-attributes --queue-url http://localhost:4566/000000000000/event-processing-queue --attribute-names QueueArn --region us-east-1 --query 'Attributes.QueueArn' --output text)
TOPIC_ARN=$(aws --endpoint-url=$ENDPOINT sns list-topics --region us-east-1 --query 'Topics[?contains(TopicArn, `event-notifications`)].TopicArn' --output text)

aws --endpoint-url=$ENDPOINT sns subscribe --topic-arn $TOPIC_ARN --protocol sqs --notification-endpoint $QUEUE_ARN --region us-east-1

echo "LocalStack setup completed successfully!"
echo "SQS Queues:"
aws --endpoint-url=$ENDPOINT sqs list-queues --region us-east-1
echo ""
echo "SNS Topics:"
aws --endpoint-url=$ENDPOINT sns list-topics --region us-east-1
