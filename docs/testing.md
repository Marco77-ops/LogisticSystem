# LuckyPets Logistics System - Testing Strategy

This document outlines the testing strategy for the LuckyPets Logistics System, including the types of tests, test coverage, and testing tools used.

## Testing Levels

### Unit Tests

Unit tests verify the functionality of individual components in isolation, using mocks or stubs for dependencies.

#### Examples:

- **Service Layer Tests**: Verify business logic in service classes
- **Event Model Tests**: Verify serialization/deserialization of events
- **Repository Tests**: Verify data access logic

#### Tools:
- JUnit 5
- Mockito
- AssertJ

### Integration Tests

Integration tests verify the interaction between components, including external dependencies like Kafka and databases.

#### Examples:

- **Kafka Producer Tests**: Verify events are correctly published to Kafka
- **Kafka Consumer Tests**: Verify events are correctly consumed from Kafka
- **REST Controller Tests**: Verify REST endpoints behave correctly
- **Repository Integration Tests**: Verify interaction with the database

#### Tools:
- Spring Boot Test
- TestContainers
- EmbeddedKafka
- MockMvc/WebTestClient

### End-to-End Tests

End-to-end tests verify complete workflows across multiple services, ensuring the system works correctly as a whole.

#### Examples:

- **Shipment Creation to Delivery Flow**: Verify a shipment can be created, scanned, and delivered
- **Analytics Generation Flow**: Verify analytics are correctly generated from delivery events

#### Tools:
- RestAssured
- Docker Compose for test environment

## Test Coverage

The testing strategy aims to achieve the following coverage:

- **Unit Tests**: >80% code coverage for business logic
- **Integration Tests**: All Kafka producers/consumers and REST endpoints
- **End-to-End Tests**: All critical business workflows

## Test Implementation by Service

### Shipment Service

#### Unit Tests:
- ShipmentService class
- Event serialization/deserialization
- Input validation

#### Integration Tests:
- ShipmentController endpoints
- ShipmentEventProducer

### Scan Service

#### Unit Tests:
- ScanService class
- Event serialization/deserialization
- Input validation

#### Integration Tests:
- ScanController endpoints
- ShipmentEventListener
- ShipmentScannedEvent producer

### Delivery Service

#### Unit Tests:
- DeliveryService class
- Event serialization/deserialization
- Input validation

#### Integration Tests:
- DeliveryController endpoints
- ShipmentScannedEventListener
- ShipmentDeliveredEvent producer

### Analytics Service

#### Unit Tests:
- Kafka Streams topology logic
- Event serialization/deserialization

#### Integration Tests:
- Kafka Streams topology with TopologyTestDriver
- Analytics REST endpoints

### Notification View Service

#### Unit Tests:
- NotificationService class
- Event handling logic

#### Integration Tests:
- NotificationController endpoints
- ShipmentEventListener

## Test Data Management

- **Test Fixtures**: Reusable test data for consistent testing
- **Test Data Builders**: Fluent builders for creating test objects
- **Database Initialization**: Scripts for initializing test databases

## Continuous Integration

Tests are run automatically as part of the CI pipeline:

1. Unit tests run on every commit
2. Integration tests run on every pull request
3. End-to-end tests run before deployment

## Test Environment

- **Local Development**: Docker Compose for running dependent services
- **CI Environment**: TestContainers for isolated test environments
- **Staging Environment**: Full deployment for end-to-end testing

## Mocking Strategy

- **External Services**: Mocked in unit tests
- **Kafka**: EmbeddedKafka for integration tests
- **Databases**: TestContainers or in-memory databases for integration tests

## Test Naming Convention

Tests follow a consistent naming convention:

```
[MethodName]_[Scenario]_[ExpectedResult]
```

Example: `createShipment_WithValidInput_ReturnsCreatedShipment`

## Test Documentation

Each test class includes JavaDoc comments explaining:

1. What is being tested
2. Test dependencies
3. Test data setup
4. Expected outcomes

## Example Test Cases

### Unit Test Example (ShipmentServiceTest)

```java
@Test
void createShipment_WithValidInput_ReturnsCreatedShipment() {
    // Arrange
    ShipmentRequest request = new ShipmentRequest("Test Destination");
    when(repository.save(any())).thenReturn(new Shipment("123", "Test Destination"));
    
    // Act
    Shipment result = shipmentService.createShipment(request);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getDestination()).isEqualTo("Test Destination");
    verify(eventProducer).sendShipmentCreatedEvent(any());
}
```

### Integration Test Example (ShipmentControllerIntegrationTest)

```java
@Test
void createShipment_WithValidInput_ReturnsCreatedShipment() {
    // Arrange
    ShipmentRequest request = new ShipmentRequest("Test Destination");
    
    // Act & Assert
    mockMvc.perform(post("/shipments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.destination").value("Test Destination"));
}
```

### Kafka Integration Test Example (ShipmentEventProducerTest)

```java
@Test
void sendShipmentCreatedEvent_PublishesEventToKafka() {
    // Arrange
    ShipmentCreatedEvent event = new ShipmentCreatedEvent(
            "test-shipment-id",
            "Test Destination",
            LocalDateTime.now(),
            "test-correlation-id"
    );
    
    // Act
    producer.sendShipmentCreatedEvent(event);
    
    // Assert
    ConsumerRecord<String, ShipmentCreatedEvent> record = 
            records.poll(Duration.ofSeconds(10));
    assertThat(record).isNotNull();
    assertThat(record.key()).isEqualTo("test-shipment-id");
    assertThat(record.value().getShipmentId()).isEqualTo("test-shipment-id");
    assertThat(record.value().getDestination()).isEqualTo("Test Destination");
}
```