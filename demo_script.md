# LuckyPets Logistics System - Demonstration Script

## Overview
This script demonstrates the end-to-end flow of the LuckyPets Logistics System, an event-driven microservice architecture using Apache Kafka. The demonstration follows a shipment from creation through delivery, showing how events flow between services and trigger actions.

## Prerequisites
- Docker and Docker Compose installed
- cURL or Postman for API calls

## Step 1: Start the System
Start all services using Docker Compose:

```bash
docker-compose up -d
```

Verify all services are running:

```bash
docker-compose ps
```

You should see all services (shipmentservice, scanservice, deliveryservice, analyticservice, notificationviewservice) running along with Kafka, Zookeeper, and PostgreSQL.

## Step 2: Create a Shipment
Create a new shipment by sending a POST request to the Shipment Service:

```bash
curl -X POST http://localhost:8081/api/v1/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Berlin",
    "destination": "Munich",
    "customerId": "customer123"
  }'
```

**Explanation:** This creates a new shipment in the system. The Shipment Service will:
1. Create a shipment record
2. Generate a ShipmentCreatedEvent
3. Publish the event to the shipment-created Kafka topic

You can check the logs to see the event being published:

```bash
docker-compose logs -f shipmentservice
```

## Step 3: Scan the Shipment at Origin
Scan the shipment at its origin location by sending a POST request to the Scan Service's `/api/v1/scans` endpoint. Replace `{shipmentId}` with the ID from the previous step and include the location in the JSON body:

```bash
curl -X POST http://localhost:8082/api/v1/scans \
  -H "Content-Type: application/json" \
  -d '{"shipmentId":"{shipmentId}","location":"Berlin"}'
```

**Explanation:** This records a scan of the shipment at Berlin. The Scan Service will:
1. Record the scan
2. Generate a ShipmentScannedEvent
3. Publish the event to the shipment-scanned Kafka topic

You can check the logs to see the event being published:

```bash
docker-compose logs -f scanservice
```

## Step 4: Scan the Shipment at Destination
Scan the shipment at its destination location by sending another POST request to the Scan Service's `/api/v1/scans` endpoint with a JSON body:

```bash
curl -X POST http://localhost:8082/api/v1/scans \
  -H "Content-Type: application/json" \
  -d '{"shipmentId":"{shipmentId}","location":"Munich"}'
```

**Explanation:** This records a scan of the shipment at Munich, which is its destination. The Scan Service will:
1. Record the scan
2. Generate a ShipmentScannedEvent
3. Publish the event to the shipment-scanned Kafka topic

Since the scan location matches the destination, the Delivery Service will:
1. Receive the ShipmentScannedEvent
2. Update the shipment status to DELIVERED
3. Generate a ShipmentDeliveredEvent
4. Publish the event to the shipment-delivered Kafka topic

You can check the logs to see these events:

```bash
docker-compose logs -f scanservice
docker-compose logs -f deliveryservice
```

## Step 5: Check Notifications
Check the notifications generated by the system:

```bash
curl -X GET http://localhost:8085/api/notifications
```

**Explanation:** The Notification View Service consumes events from all topics and creates notifications. You should see notifications for:
1. Shipment creation
2. Shipment scan at Berlin
3. Shipment scan at Munich
4. Shipment delivery

You can also check notifications for a specific shipment:

```bash
curl -X GET "http://localhost:8085/api/notifications/shipment/{shipmentId}"
```

## Step 6: Check Delivery Status
Check the delivery status of the shipment:

```bash
curl -X GET "http://localhost:8083/deliveries/{shipmentId}"
```

**Explanation:** This shows the current status of the shipment, which should be DELIVERED.

## Step 7: Observe Analytics Processing
The Analytics Service processes ShipmentDeliveredEvent and updates its local analytics state using Kafka Streams. While there's no direct API to view raw stream output, you can observe the processing in the logs:

```bash
docker-compose logs -f analyticservice
```

You should see log entries showing the ShipmentDeliveredEvent being processed and the aggregated counts being stored.

## Step 8: Monitor Kafka Topics
You can use the Kafka UI to monitor the topics and messages:

1. Open http://localhost:8080 in your browser
2. Navigate to the Topics section
3. Explore the messages in each topic:
   - shipment-created
   - shipment-scanned
   - shipment-delivered
   - shipment-analytics

## Error Handling Demonstration (Optional)
To demonstrate error handling, you can try:

1. Scanning a non-existent shipment by calling the `/api/v1/scans` endpoint:

```bash
curl -X POST http://localhost:8082/api/v1/scans \
  -H "Content-Type: application/json" \
  -d '{"shipmentId":"nonexistent","location":"Berlin"}'
```

This should return an error message.

2. Stopping a service and observing retry behavior:

```bash
docker-compose stop notificationviewservice
```

Create or scan a shipment, then restart the service:

```bash
docker-compose start notificationviewservice
```

Check the logs to see how the service handles the backlog of events:

```bash
docker-compose logs -f notificationviewservice
```

## Conclusion
This demonstration shows the complete flow of a shipment through the LuckyPets Logistics System:

1. Shipment creation
2. Scanning at origin
3. Scanning at destination
4. Automatic delivery processing
5. Notification generation
6. Analytics processing

The event-driven architecture allows services to operate independently while maintaining consistency through Kafka events.