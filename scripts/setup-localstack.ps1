# PowerShell script for setting up LocalStack on Windows

Write-Host "Setting up LocalStack resources..." -ForegroundColor Green

# Wait for LocalStack to be ready
Write-Host "Waiting for LocalStack to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Set LocalStack endpoint
$ENDPOINT = "http://localhost:4566"
$REGION = "us-east-1"

# Create SQS Queues
Write-Host "Creating SQS queues..." -ForegroundColor Cyan
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name event-processing-queue --region $REGION
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name event-dlq --region $REGION
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name high-priority-queue --region $REGION

# Create SNS Topics
Write-Host "Creating SNS topics..." -ForegroundColor Cyan
aws --endpoint-url=$ENDPOINT sns create-topic --name event-notifications --region $REGION
aws --endpoint-url=$ENDPOINT sns create-topic --name alert-notifications --region $REGION

# Create CloudWatch Log Groups
Write-Host "Creating CloudWatch log groups..." -ForegroundColor Cyan
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /aws/lambda/event-processor --region $REGION
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /events/processing --region $REGION
aws --endpoint-url=$ENDPOINT logs create-log-group --log-group-name /events/errors --region $REGION

# Subscribe SQS to SNS
Write-Host "Subscribing SQS to SNS topics..." -ForegroundColor Cyan
$QUEUE_ARN = aws --endpoint-url=$ENDPOINT sqs get-queue-attributes --queue-url "http://localhost:4566/000000000000/event-processing-queue" --attribute-names QueueArn --region $REGION --query "Attributes.QueueArn" --output text
$TOPIC_ARN = aws --endpoint-url=$ENDPOINT sns list-topics --region $REGION --query "Topics[?contains(TopicArn, 'event-notifications')].TopicArn" --output text

aws --endpoint-url=$ENDPOINT sns subscribe --topic-arn $TOPIC_ARN --protocol sqs --notification-endpoint $QUEUE_ARN --region $REGION

Write-Host "LocalStack setup completed successfully!" -ForegroundColor Green
Write-Host "`nSQS Queues:" -ForegroundColor Yellow
aws --endpoint-url=$ENDPOINT sqs list-queues --region $REGION
Write-Host "`nSNS Topics:" -ForegroundColor Yellow
aws --endpoint-url=$ENDPOINT sns list-topics --region $REGION
