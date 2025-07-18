# LuckyPets Logistics System

## Inhaltsverzeichnis
- [Überblick](#überblick)
- [Architektur](#architektur)
- [Event-Modell](#event-modell)
- [Microservices & Komponenten](#microservices--komponenten)
- [Kafka Streams Analytics](#kafka-streams-analytics)
- [REST-APIs](#rest-apis)
- [Containerisierung & Orchestrierung](#containerisierung--orchestrierung)
- [Testing](#testing)
- [AWS Lambda & Serverless](#aws-lambda--serverless)
- [Erweiterbarkeit](#erweiterbarkeit)
- [ToDos & Verbesserungen](#todos--verbesserungen)
- [API Dokumentation](#api-dokumentation)

## Überblick
Das LuckyPets Logistics System ist eine Microservice-basierte Event-Streaming-Plattform für die Verwaltung und Nachverfolgung von Tier-Transporten. Die Services kommunizieren ausschließlich asynchron über Apache Kafka. Die Architektur ist auf Erweiterbarkeit, Fehlertoleranz und Echtzeit-Analysen ausgelegt.

## Architektur

### Übersicht
- **Event-Driven Microservices**: Jeder Service ist unabhängig deploybar und kommuniziert über Kafka-Events.
- **Kafka als Backbone**: Alle Business-Events werden über Topics verteilt.
- **Read-Model-Services**: Analytics- und NotificationView-Service sind reine Konsumenten und bieten REST-APIs für UI/Admin.
- **Serverless-Integration**: AWS Lambda für Benachrichtigungen (E-Mail/SMS) via Webhook.

### Komponenten-Diagramm
```
ShipmentService ──▶ ScanService ──▶ DeliveryService ──▶ AnalyticsService
      │                │                 │                    │
      ▼                ▼                 ▼                    ▼
NotificationViewService (liest alle Events)
AWS Lambda (per Webhook von NotificationViewService)
```

## Event-Modell
Alle Events erben von `AbstractEvent` (implementiert `BaseEvent`).
- **Felder:**
  - `correlationId`, `timestamp`, `version`, `eventType`
- **Events:**
  - `ShipmentCreatedEvent` (shipmentId, destination, createdAt)
  - `ShipmentScannedEvent` (shipmentId, location, scannedAt, destination)
  - `ShipmentDeliveredEvent` (shipmentId, destination, location, deliveredAt)

## Microservices & Komponenten
- **shipmentservice**: Erstellt Sendungen, produziert ShipmentCreatedEvent
- **scanservice**: Verarbeitet Scans, produziert ShipmentScannedEvent, konsumiert ShipmentCreatedEvent
- **deliveryservice**: Markiert Sendungen als zugestellt, produziert ShipmentDeliveredEvent, konsumiert ShipmentScannedEvent
- **analyticservice**: Aggregiert ShipmentDeliveredEvents mit Kafka Streams, REST-API für Analytics
- **notificationviewservice**: Konsumiert alle Events, bietet REST-API für Benachrichtigungen, triggert AWS Lambda Webhook
- **shared**: Gemeinsame Event-Modelle und Utility-Klassen
- **e2e-tests**: End-to-End-Tests für komplette Workflows
- **aws-lambda**: Serverless Funktion für E-Mail/SMS-Benachrichtigung (SES/SNS)

## Kafka Streams Analytics
- **analyticservice** nutzt Kafka Streams für Aggregationen (z.B. Lieferungen pro Stunde/Ort)
- Aggregierte Daten werden im lokalen State Store gehalten und per REST bereitgestellt

## REST-APIs
Jeder Service bietet REST-Endpunkte (Swagger-UI verfügbar):
- shipmentservice: `/api/v1/shipments`
- scanservice: `/api/v1/scans`
- deliveryservice: `/api/v1/deliveries`
- analyticservice: `/api/analytics`
- notificationviewservice: `/api/notifications`

## Containerisierung & Orchestrierung
- **Docker Compose** orchestriert alle Services, Kafka, Zookeeper, Kafka UI
- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- Kafka UI: http://localhost:8080

## Testing
- **Unit-Tests**: Business-Logik pro Service (JUnit, Mockito)
- **Integration-Tests**: Kafka Producer/Consumer, REST-Endpoints (Testcontainers, EmbeddedKafka)
- **End-to-End-Tests**: Komplette Workflows im e2e-tests Modul (RestAssured, Docker Compose)
- Siehe auch: `docs/testing.md`

## AWS Lambda & Serverless
- **aws-lambda**-Modul: Node.js Lambda für E-Mail (SES) und SMS (SNS)
- Webhook-URL wird vom NotificationViewService aufgerufen
- Beispiel-Event: `{ eventType, shipmentId, customerId, origin, destination, timestamp, correlationId }`

## Erweiterbarkeit
- Neue Events: Einfach durch Ableitung von `AbstractEvent`
- Neue Services: An Kafka anbinden, Events konsumieren/produzieren
- Analytics: Neue Kafka Streams Topologien im analyticservice
- Notification Channels: Erweiterung der Lambda oder NotificationViewService

## ToDos & Verbesserungen
Siehe `docs/tasks.md` für aktuelle Verbesserungs- und Feature-Listen (z.B. Auth, Monitoring, Dead Letter Queues, API-Doku, Security, Testabdeckung).

---

**Letztes Update:** 10.07.2025
