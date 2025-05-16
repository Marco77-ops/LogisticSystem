# LuckyPets Logistics System

## Overview
The LuckyPets Logistics System is a microservice-based logistics management system designed for tracking pet shipments. It uses an event-driven architecture with Apache Kafka for asynchronous communication between services.

## Architecture

### Microservice Architecture
The system is composed of the following microservices:

1. **Shipment Service**: Creates new shipments and publishes shipment creation events
2. **Scan Service**: Records scanning events when shipments are scanned at various locations
3. **Delivery Service**: Manages the delivery of shipments to their final destinations
4. **Analytics Service**: Aggregates and analyzes shipment events using Kafka Streams
5. **Notification Service**: Consumes events and provides notifications via REST endpoints

### Event-Driven Communication
Services communicate asynchronously by publishing and consuming events via Apache Kafka. Each key business action triggers an event:

- **ShipmentCreatedEvent**: When a new shipment is created
- **ShipmentScannedEvent**: When a shipment is scanned at a location
- **ShipmentDeliveredEvent**: When a shipment is delivered to its destination
- **ShipmentAnalyticsEvent**: Aggregated analytics data

### Event Flow Diagram
```
┌─────────────────┐     ShipmentCreatedEvent     ┌─────────────────┐
│                 │ ─────────────────────────────▶                 │
│ Shipment Service│                              │   Scan Service  │
│                 │                              │                 │
└─────────────────┘                              └────────┬────────┘
                                                          │
                                                          │ ShipmentScannedEvent
                                                          │
                                                          ▼
┌─────────────────┐     ShipmentDeliveredEvent   ┌─────────────────┐
│                 │ ◀─────────────────────────────                 │
│ Notification    │                              │ Delivery Service│
│    Service      │                              │                 │
└─────────────────┘                              └────────┬────────┘
                                                          │
                                                          │ ShipmentDeliveredEvent
                                                          │
                                                          ▼
                                                 ┌─────────────────┐
                                                 │                 │
                                                 │ Analytics Service│
                                                 │                 │
                                                 └─────────────────┘
```

## Standardized Event Model
All events inherit from a shared base class (`AbstractEvent`) which implements the `BaseEvent` interface. Common fields include:
- `correlationId`: For tracking related events
- `timestamp`: When the event occurred
- `version`: Event schema version
- `aggregateId`: ID of the entity the event relates to (e.g., shipmentId)
- `eventType`: Type of the event

## Kafka Streams Analytics
The Analytics Service uses Kafka Streams to process and aggregate shipment events:
- Counts deliveries per hour and location
- Publishes aggregated data as `ShipmentAnalyticsEvent`

### Kafka Streams Topology
```
ShipmentDeliveredEvent Stream
         │
         ▼
Group by location
         │
         ▼
Window by 1 hour
         │
         ▼
Count events in window
         │
         ▼
Map to ShipmentAnalyticsEvent
         │
         ▼
Publish to shipment-analytics topic
```

## REST Interfaces
Each service provides REST endpoints for triggering actions and querying state:

- **Shipment Service**: Create shipments
- **Scan Service**: Record shipment scans
- **Delivery Service**: Mark shipments as delivered
- **Analytics Service**: Query analytics data
- **Notification Service**: Query notifications

## Containerization & Orchestration
All services are containerized using Docker and orchestrated via docker-compose for local development and testing. The system includes:
- Kafka and Zookeeper containers
- Kafka UI for monitoring
- PostgreSQL database
- Service containers

## Testing Strategy
The system includes various levels of testing:
- **Unit Tests**: For core business logic
- **Integration Tests**: For Kafka producers/consumers and REST endpoints
- **End-to-End Tests**: For complete event flows

## Error Handling and Robustness
Services handle errors gracefully with:
- Proper exception handling
- Logging of errors
- Retry mechanisms for Kafka operations

## Extensibility
The architecture allows for easy addition of new services or event types by:
- Using a standardized event model
- Loose coupling between services
- Event-driven communication

## Getting Started
1. Clone the repository
2. Run `docker-compose up` to start all services
3. Use the REST endpoints to interact with the system

## API Documentation
For detailed API documentation, see the Swagger UI available at:
- Shipment Service: http://localhost:8081/swagger-ui.html
- Scan Service: http://localhost:8082/swagger-ui.html
- Delivery Service: http://localhost:8083/swagger-ui.html
- Analytics Service: http://localhost:8084/swagger-ui.html
- Notification Service: http://localhost:8085/swagger-ui.html