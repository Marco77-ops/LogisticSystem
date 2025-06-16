# LuckyPets Logistics System

## Motivation & Anwendungsgebiete von Event Streaming mit Apache Kafka

Moderne Unternehmen stehen vor der Herausforderung, immer größere Datenmengen in Echtzeit zu verarbeiten und flexibel auf Ereignisse reagieren zu können. **Event Streaming** mit Apache Kafka ist mittlerweile ein zentraler Baustein vieler Software-Architekturen.

**Was kann ich mit Event Streaming (Kafka) machen?**
Laut der offiziellen Dokumentation von Apache Kafka werden Event-Streaming-Plattformen für vielfältige Anwendungsfälle eingesetzt, zum Beispiel:
- **Messaging:** Zuverlässige, skalierbare Kommunikation zwischen Applikationen
- **Website-Aktivitäten tracken:** Z. B. Klicks, Anmeldungen, Nutzeraktionen in Echtzeit analysieren
- **Metriken sammeln:** System- und Applikationsmetriken aggregieren
- **Logaggregation:** Zentrale Sammlung und Verarbeitung von Logs aus unterschiedlichen Systemen
- **Stream Processing:** Ereignisse fortlaufend analysieren und transformieren (z. B. für Analytics)
- **Event Sourcing:** Änderungen am Systemzustand über Events nachvollziehen
- **Integration externer Systeme:** Brücke zwischen Datenbanken, Cloud-Services und anderen Plattformen

**Moderne Erweiterung: Serverless Functions**
Ein zunehmend wichtiger Anwendungsfall ist die Verbindung von Kafka mit **Serverless Functions** (wie AWS Lambda oder Azure Functions). Hierbei dienen Kafka-Topics als Eventquelle für Functions, die automatisch skalieren und flexibel beliebige Aufgaben übernehmen – etwa Benachrichtigungen, Datenanalysen oder das Anstoßen von Workflows.

**Fazit:**  
Event Streaming mit Apache Kafka ist nicht nur ein Spezialfall für große Unternehmen, sondern ein universell einsetzbares Architekturpattern für zahlreiche moderne Anwendungen – von Web-Analytics bis zur Echtzeit-Verarbeitung von IoT-Daten und serverlosen Architekturen.

## Zentrale Konzepte: Topics & Partitions

Das Herzstück von Kafka ist die Organisation von Daten in **Topics**. Ein Topic ist ein logischer Kanal, auf dem Events veröffentlicht und von Konsumenten abonniert werden.  
Um Skalierbarkeit und Fehlertoleranz zu gewährleisten, wird jedes Topic in mehrere **Partitions** unterteilt.  
Dadurch können viele Konsumenten parallel Events lesen und verarbeiten – ein zentrales Element der Kafka-Architektur.


## Overview
The LuckyPets Logistics System is a microservice-based logistics management system designed for tracking pet shipments. It uses an event-driven architecture with Apache Kafka for asynchronous communication between services.

## Architecture

### Microservice Architecture
The system is composed of the following microservices:

1. **Shipment Service**: Creates new shipments and publishes shipment creation events
2. **Scan Service**: Records scanning events when shipments are scanned at various locations
3. **Delivery Service**: Manages the delivery of shipments to their final destinations
4. **Analytics Service**: Aggregates and analyzes shipment events using Kafka Streams
5. **Notification View Service**: Consumes events and exposes notifications for the UI via REST

### Event-Driven Communication
Services communicate asynchronously by publishing and consuming events via Apache Kafka. Each key business action triggers an event:

- **ShipmentCreatedEvent**: When a new shipment is created
- **ShipmentScannedEvent**: When a shipment is scanned at a location
- **ShipmentDeliveredEvent**: When a shipment is delivered to its destination

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
Persist counts in local state store
```

## REST Interfaces
Each service provides REST endpoints for triggering actions and querying state:

- **Shipment Service**: Create shipments
- **Scan Service**: Record shipment scans
- **Delivery Service**: Mark shipments as delivered
- **Analytics Service**: Query analytics data
- **Notification View Service**: Query notifications

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
   ```bash
   git clone https://github.com/yourusername/luckypets-logistics.git
   cd luckypets-logistics
   ```

2. Start all services using Docker Compose
   ```bash
   docker-compose up -d
   ```

3. Verify all services are running
   ```bash
   docker-compose ps
   ```

4. Test the system with the following example flow:

   a. Create a shipment
   ```bash
   curl -X POST http://localhost:8081/api/v1/shipments \
     -H "Content-Type: application/json" \
     -d '{
       "origin": "Berlin",
       "destination": "Munich",
       "customerId": "customer123"
     }'
   ```

   b. Scan the shipment at origin (replace {shipmentId} with the ID from the previous step)
   ```bash
   curl -X POST http://localhost:8082/api/v1/scans \
     -H "Content-Type: application/json" \
     -d '{"shipmentId":"{shipmentId}","location":"Berlin"}'
   ```

   c. Scan the shipment at destination
   ```bash
   curl -X POST http://localhost:8082/api/v1/scans \
     -H "Content-Type: application/json" \
     -d '{"shipmentId":"{shipmentId}","location":"Munich"}'
   ```

   d. Check notifications
   ```bash
   curl -X GET http://localhost:8085/api/notifications
   ```

   e. Check delivery status
   ```bash
   curl -X GET "http://localhost:8083/deliveries/{shipmentId}"
   ```

5. Monitor Kafka topics using the Kafka UI at http://localhost:8080

6. To stop all services
   ```bash
   docker-compose down
   ```

## Testing
The system includes various levels of testing:

### Unit Tests
Unit tests verify the core business logic of each service in isolation, using mocks for external dependencies.

To run unit tests for a specific service:
```bash
cd [service-directory]  # e.g., cd shipmentservice
./mvnw test -Dtest=*UnitTest
```

### Integration Tests
Integration tests verify the interaction between components, including Kafka producers/consumers and REST endpoints.

To run integration tests for a specific service:
```bash
cd [service-directory]  # e.g., cd shipmentservice
./mvnw test -Dtest=*IntegrationTest
```

### End-to-End Tests
The demo script in `demo_script.md` provides a comprehensive end-to-end test scenario that can be executed manually.

## API Documentation
For detailed API documentation, see the Swagger UI available at:
- Shipment Service: http://localhost:8081/swagger-ui.html
- Scan Service: http://localhost:8082/swagger-ui.html
- Delivery Service: http://localhost:8083/swagger-ui.html
- Analytics Service: http://localhost:8084/swagger-ui.html
- Notification View Service: http://localhost:8085/swagger-ui.html

## Reflection & Outlook

### Architectural Strengths
- **Loose Coupling**: The event-driven architecture allows services to evolve independently
- **Scalability**: Each service can be scaled independently based on load
- **Resilience**: Services can continue to function even if other services are temporarily unavailable
- **Flexibility**: New services can be added without modifying existing ones

### Challenges & Limitations
1. **Local Development Complexity**: 
   - Running the full system locally requires significant resources
   - Debugging across service boundaries can be challenging

2. **Event Versioning**: 
   - As the system evolves, managing event schema changes becomes critical
   - Need for a strategy to handle backward/forward compatibility

3. **Error Handling**: 
   - Distributed error handling requires careful design
   - Need for monitoring and alerting across services

4. **Testing Complexity**:
   - End-to-end testing requires orchestrating multiple services
   - Integration tests with Kafka add complexity

### Future Enhancements

1. **Enhanced Analytics**:
   - Real-time dashboards for logistics metrics
   - Predictive analytics for delivery time estimation
   - Anomaly detection for unusual shipment patterns

2. **Monitoring & Observability**:
   - Distributed tracing across services
   - Centralized logging with ELK stack
   - Prometheus and Grafana for metrics visualization

3. **Serverless Extensions**:
   - AWS Lambda functions triggered by Kafka events for:
     - Customer notifications (SMS, email)
     - Fraud detection
     - Billing and invoicing

4. **Cloud Deployment**:
   - Kubernetes deployment for production
   - Auto-scaling based on traffic patterns
   - Multi-region deployment for disaster recovery

5. **Additional Features**:
   - Customer-facing tracking portal
   - Mobile app for delivery personnel
   - Integration with external logistics providers
