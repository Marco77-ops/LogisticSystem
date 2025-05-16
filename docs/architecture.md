# LuckyPets Logistics System - Architecture Documentation

## System Architecture Overview

The LuckyPets Logistics System is built as a set of microservices that communicate via Apache Kafka. This document provides detailed information about the system architecture, event flows, and component interactions.

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐             │
│  │             │      │             │      │             │             │
│  │  Shipment   │      │    Scan     │      │  Delivery   │             │
│  │  Service    │      │   Service   │      │  Service    │             │
│  │             │      │             │      │             │             │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘             │
│         │                    │                    │                     │
│         │                    │                    │                     │
│         ▼                    ▼                    ▼                     │
│  ┌─────────────────────────────────────────────────────────────┐       │
│  │                                                             │       │
│  │                      Apache Kafka                           │       │
│  │                                                             │       │
│  └─────────────────────────┬───────────────────────────────────┘       │
│                            │                                           │
│                            │                                           │
│         ┌─────────────────┐│┌────────────────┐                         │
│         │                 ││                 │                         │
│         │  Notification   ◀┘│   Analytics    │                         │
│         │    Service      │ │   Service      │                         │
│         │                 │ │                │                         │
│         └─────────────────┘ └────────────────┘                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## Kafka Topics

| Topic Name | Producer | Consumers | Description |
|------------|----------|-----------|-------------|
| shipment-created | Shipment Service | Scan Service, Notification Service | Published when a new shipment is created |
| shipment-scanned | Scan Service | Delivery Service, Notification Service | Published when a shipment is scanned at a location |
| shipment-delivered | Delivery Service | Analytics Service, Notification Service | Published when a shipment is delivered |
| shipment-analytics | Analytics Service | (None) | Contains aggregated analytics data |

## Event Model

All events in the system inherit from the `AbstractEvent` class, which implements the `BaseEvent` interface:

```
┌───────────────┐
│  «interface»  │
│   BaseEvent   │
└───────┬───────┘
        │
        │
┌───────▼───────┐
│ AbstractEvent │
└───────┬───────┘
        │
        ├─────────────────┬─────────────────┬─────────────────┐
        │                 │                 │                 │
┌───────▼───────┐ ┌───────▼───────┐ ┌───────▼───────┐ ┌───────▼───────┐
│ ShipmentCreated│ │ShipmentScanned│ │ShipmentDelivered│ │ShipmentAnalytics│
│     Event     │ │     Event     │ │     Event     │ │     Event     │
└───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘
```

## Service Descriptions

### Shipment Service
- **Responsibility**: Creating and managing shipments
- **Endpoints**: 
  - `POST /shipments` - Create a new shipment
  - `GET /shipments/{id}` - Get shipment details
- **Events Published**: ShipmentCreatedEvent
- **Events Consumed**: None

### Scan Service
- **Responsibility**: Recording shipment scans at various locations
- **Endpoints**: 
  - `POST /scans` - Record a shipment scan
  - `GET /scans/shipment/{shipmentId}` - Get scan history for a shipment
- **Events Published**: ShipmentScannedEvent
- **Events Consumed**: ShipmentCreatedEvent

### Delivery Service
- **Responsibility**: Managing shipment deliveries
- **Endpoints**: 
  - `POST /deliveries` - Mark a shipment as delivered
  - `GET /deliveries/shipment/{shipmentId}` - Get delivery details
- **Events Published**: ShipmentDeliveredEvent
- **Events Consumed**: ShipmentScannedEvent

### Analytics Service
- **Responsibility**: Aggregating and analyzing shipment data
- **Endpoints**: 
  - `GET /analytics/deliveries/hourly` - Get hourly delivery counts
  - `GET /analytics/deliveries/location` - Get delivery counts by location
- **Events Published**: ShipmentAnalyticsEvent
- **Events Consumed**: ShipmentDeliveredEvent
- **Kafka Streams**: Processes ShipmentDeliveredEvent to generate analytics

### Notification Service
- **Responsibility**: Managing notifications for shipment events
- **Endpoints**: 
  - `GET /api/notifications` - Get all notifications
  - `GET /api/notifications/shipment/{shipmentId}` - Get notifications for a shipment
- **Events Published**: None
- **Events Consumed**: ShipmentCreatedEvent, ShipmentScannedEvent, ShipmentDeliveredEvent

## Kafka Streams Topology (Analytics Service)

The Analytics Service uses Kafka Streams to process ShipmentDeliveredEvent and generate aggregated analytics:

```
┌───────────────────────┐
│                       │
│ ShipmentDeliveredEvent│
│       Stream          │
│                       │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│                       │
│    Group by location  │
│                       │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│                       │
│   Window by 1 hour    │
│                       │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│                       │
│ Count events in window│
│                       │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│                       │
│ Map to Analytics Event│
│                       │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│                       │
│ shipment-analytics    │
│       topic           │
│                       │
└───────────────────────┘
```

## Deployment Architecture

The system is deployed using Docker containers orchestrated with docker-compose:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Docker Compose                             │
│                                                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐ │
│  │Zookeeper│  │  Kafka  │  │ Kafka UI│  │PostgreSQL│  │pgAdmin │ │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └────────┘ │
│                                                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐ │
│  │Shipment │  │  Scan   │  │Delivery │  │Analytics│  │Notific.│ │
│  │Service  │  │Service  │  │Service  │  │Service  │  │Service │ │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Error Handling Strategy

The system implements several error handling mechanisms:

1. **Retry Mechanism**: Kafka operations use retry logic to handle temporary failures
2. **Dead Letter Queues**: Failed messages are sent to dead letter queues for later processing
3. **Circuit Breakers**: Prevent cascading failures when downstream services are unavailable
4. **Graceful Degradation**: Services continue to function with reduced capabilities when dependencies fail
5. **Comprehensive Logging**: All errors are logged with appropriate context for troubleshooting

## Extensibility

The system is designed to be easily extensible:

1. **New Event Types**: Can be added by extending AbstractEvent
2. **New Services**: Can be added by connecting to Kafka and consuming/producing relevant events
3. **New Analytics**: Can be implemented by adding new Kafka Streams topologies
4. **New Notification Channels**: Can be added to the Notification Service