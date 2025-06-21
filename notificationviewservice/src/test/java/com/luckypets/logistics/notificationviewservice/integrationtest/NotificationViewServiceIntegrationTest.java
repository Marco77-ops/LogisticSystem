package com.luckypets.logistics.notificationviewservice.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationViewServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(NotificationViewServiceIntegrationTest.class);
    private static final int KAFKA_READY_TIMEOUT = 60;
    private static final int NOTIFICATION_PROCESSING_TIMEOUT = 30;

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withStartupTimeout(java.time.Duration.ofMinutes(2));

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private KafkaProducer<String, Object> producer;
    private String testRunId;
    private static AdminClient adminClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        kafka.start();

        String bootstrapServers = kafka.getBootstrapServers();

        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        
        // Use the same group ID as the main application to ensure listeners work
        registry.add("spring.kafka.consumer.group-id", () -> "notification-view-service");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);
        
        // JSON deserialization configuration
        registry.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.deserializer.value.delegate.class", () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "com.luckypets.logistics.shared.events,com.luckypets.logistics.notificationviewservice.model");
        registry.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> true);
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type", () -> "java.lang.Object");
        
        // Producer configuration
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> true);
        
        // Listener configuration
        registry.add("spring.kafka.listener.auto-startup", () -> true);
        registry.add("spring.kafka.listener.ack-mode", () -> "manual_immediate");

        // Topic configuration
        registry.add("kafka.topic.notification-sent", () -> "notification-sent-test");

        logger.info("Configured Kafka with bootstrap-servers: {}", bootstrapServers);
    }

    @BeforeAll
    void setupKafka() throws Exception {
        // Create admin client
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        adminClient = AdminClient.create(adminProps);

        // Create topics
        createTopics();

        // Wait for topics to be ready
        await("Topics should be created")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        Set<String> topics = adminClient.listTopics().names().get();
                        return topics.contains("shipment-created") &&
                               topics.contains("shipment-scanned") &&
                               topics.contains("shipment-delivered");
                    } catch (Exception e) {
                        logger.warn("Waiting for topics: {}", e.getMessage());
                        return false;
                    }
                });

        logger.info("Topics created successfully");
    }

    private void createTopics() throws Exception {
        List<NewTopic> topics = Arrays.asList(
                new NewTopic("shipment-created", 1, (short) 1),
                new NewTopic("shipment-scanned", 1, (short) 1),
                new NewTopic("shipment-delivered", 1, (short) 1),
                new NewTopic("notification-sent-test", 1, (short) 1)
        );

        try {
            adminClient.createTopics(topics).all().get(10, TimeUnit.SECONDS);
            logger.info("Topics created: shipment-created, shipment-scanned, shipment-delivered, notification-sent-test");
        } catch (Exception e) {
            logger.info("Topics may already exist: {}", e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        testRunId = UUID.randomUUID().toString().substring(0, 8);

        setupProducer();

        logger.info("Starting test with ID: {}", testRunId);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            try {
                producer.close();
                logger.info("Producer closed for test: {}", testRunId);
            } catch (Exception e) {
                logger.warn("Error closing producer: {}", e.getMessage());
            }
        }
    }

    @AfterAll
    void cleanupKafka() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    private void setupProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        props.put("spring.json.add.type.headers", true);

        producer = new KafkaProducer<>(props);
    }

    @Test
    @DisplayName("Should process ShipmentCreatedEvent and create notification")
    void shouldProcessShipmentCreatedEventAndCreateNotification() throws Exception {
        // Arrange
        String shipmentId = "SHIP-CREATED-" + testRunId;
        String destination = "TestDestination-" + testRunId;
        LocalDateTime createdAt = LocalDateTime.now();

        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                shipmentId, destination, createdAt, "corr-" + testRunId);

        // Act
        sendEventAndVerifyDelivery("shipment-created", event, shipmentId);

        // Assert
        verifyNotificationCreated(shipmentId, NotificationType.SHIPMENT_CREATED, destination);
    }

    @Test
    @DisplayName("Should process ShipmentScannedEvent and create notification")
    void shouldProcessShipmentScannedEventAndCreateNotification() throws Exception {
        // Arrange
        String shipmentId = "SHIP-SCANNED-" + testRunId;
        String location = "TestLocation-" + testRunId;
        String destination = "TestDestination-" + testRunId;
        LocalDateTime scannedAt = LocalDateTime.now();

        ShipmentScannedEvent event = new ShipmentScannedEvent(
                shipmentId, location, scannedAt, destination, "corr-" + testRunId);

        // Act
        sendEventAndVerifyDelivery("shipment-scanned", event, shipmentId);

        // Assert
        verifyNotificationCreated(shipmentId, NotificationType.SHIPMENT_SCANNED, location);
    }

    @Test
    @DisplayName("Should process ShipmentDeliveredEvent and create notification")
    void shouldProcessShipmentDeliveredEventAndCreateNotification() throws Exception {
        // Arrange
        String shipmentId = "SHIP-DELIVERED-" + testRunId;
        String destination = "TestDestination-" + testRunId;
        LocalDateTime deliveredAt = LocalDateTime.now();

        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                shipmentId, destination, destination, deliveredAt, "corr-" + testRunId);

        // Act
        sendEventAndVerifyDelivery("shipment-delivered", event, shipmentId);

        // Assert
        verifyNotificationCreated(shipmentId, NotificationType.SHIPMENT_DELIVERED, destination);
    }

    @Test
    @DisplayName("Should handle complete shipment lifecycle and create multiple notifications")
    void shouldHandleCompleteShipmentLifecycleAndCreateMultipleNotifications() throws Exception {
        // Arrange
        String shipmentId = "SHIP-LIFECYCLE-" + testRunId;
        String destination = "LifecycleDestination-" + testRunId;
        String scanLocation = "ScanLocation-" + testRunId;
        LocalDateTime baseTime = LocalDateTime.now();

        // Act & Assert - Send events in order
        // 1. Shipment Created
        ShipmentCreatedEvent createdEvent = new ShipmentCreatedEvent(
                shipmentId, destination, baseTime, "corr-created-" + testRunId);
        sendEventAndVerifyDelivery("shipment-created", createdEvent, shipmentId);
        verifyNotificationCreated(shipmentId, NotificationType.SHIPMENT_CREATED, destination);

        // 2. Shipment Scanned
        ShipmentScannedEvent scannedEvent = new ShipmentScannedEvent(
                shipmentId, scanLocation, baseTime.plusMinutes(30), destination, "corr-scanned-" + testRunId);
        sendEventAndVerifyDelivery("shipment-scanned", scannedEvent, shipmentId);

        // 3. Shipment Delivered
        ShipmentDeliveredEvent deliveredEvent = new ShipmentDeliveredEvent(
                shipmentId, destination, destination, baseTime.plusHours(2), "corr-delivered-" + testRunId);
        sendEventAndVerifyDelivery("shipment-delivered", deliveredEvent, shipmentId);

        // Verify all notifications exist for this shipment
        await("Should have 3 notifications for shipment")
                .atMost(NOTIFICATION_PROCESSING_TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationService.findByShipmentId(shipmentId);
                    assertThat(notifications).hasSize(3);

                    // Verify notification types
                    Set<NotificationType> types = new HashSet<>();
                    for (Notification notification : notifications) {
                        types.add(notification.getType());
                    }
                    assertThat(types).containsExactlyInAnyOrder(
                            NotificationType.SHIPMENT_CREATED,
                            NotificationType.SHIPMENT_SCANNED,
                            NotificationType.SHIPMENT_DELIVERED
                    );
                });
    }

    @Test
    @DisplayName("Should expose REST API endpoints correctly")
    void shouldExposeRestApiEndpointsCorrectly() throws Exception {
        // Arrange - Create a test notification first
        String shipmentId = "SHIP-API-" + testRunId;
        String destination = "ApiTestDestination-" + testRunId;

        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                shipmentId, destination, LocalDateTime.now(), "corr-api-" + testRunId);
        sendEventAndVerifyDelivery("shipment-created", event, shipmentId);

        // Wait for notification to be processed
        await("Notification should be created")
                .atMost(NOTIFICATION_PROCESSING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !notificationService.findByShipmentId(shipmentId).isEmpty());

        // Act & Assert - Test REST endpoints
        String baseUrl = "http://localhost:" + port + "/api/notifications";

        // 1. Get all notifications
        ResponseEntity<List<Notification>> allNotificationsResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Notification>>() {}
        );
        assertThat(allNotificationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allNotificationsResponse.getBody()).isNotEmpty();

        // 2. Get notifications by shipment ID
        ResponseEntity<List<Notification>> shipmentNotificationsResponse = restTemplate.exchange(
                baseUrl + "/shipment/" + shipmentId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Notification>>() {}
        );
        assertThat(shipmentNotificationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shipmentNotificationsResponse.getBody()).hasSize(1);

        Notification notification = shipmentNotificationsResponse.getBody().get(0);
        assertThat(notification.getShipmentId()).isEqualTo(shipmentId);
        assertThat(notification.getType()).isEqualTo(NotificationType.SHIPMENT_CREATED);

        // 3. Get notification by ID
        String notificationId = notification.getId();
        ResponseEntity<Notification> singleNotificationResponse = restTemplate.getForEntity(
                baseUrl + "/" + notificationId,
                Notification.class
        );
        assertThat(singleNotificationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(singleNotificationResponse.getBody().getId()).isEqualTo(notificationId);

        // 4. Test non-existent notification
        ResponseEntity<Notification> notFoundResponse = restTemplate.getForEntity(
                baseUrl + "/non-existent-id",
                Notification.class
        );
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should create test notification via REST API")
    void shouldCreateTestNotificationViaRestApi() {
        // Arrange
        String shipmentId = "SHIP-TEST-API-" + testRunId;
        String message = "Test notification message";
        String baseUrl = "http://localhost:" + port + "/api/notifications";

        // Act
        ResponseEntity<Notification> response = restTemplate.postForEntity(
                baseUrl + "/test?shipmentId=" + shipmentId + "&message=" + message,
                null,
                Notification.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getShipmentId()).isEqualTo(shipmentId);
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getType()).isEqualTo(NotificationType.SHIPMENT_CREATED);

        // Verify notification was saved
        List<Notification> savedNotifications = notificationService.findByShipmentId(shipmentId);
        assertThat(savedNotifications).hasSize(1);
        assertThat(savedNotifications.get(0).getMessage()).isEqualTo(message);
    }

    private void sendEventAndVerifyDelivery(String topic, Object event, String shipmentId)
            throws ExecutionException, InterruptedException, TimeoutException {

        logger.debug("Sending event to topic {}: {}", topic, shipmentId);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, shipmentId, event);
        producer.send(record).get(10, TimeUnit.SECONDS);
        producer.flush();

        logger.debug("Event sent successfully to topic {}: {}", topic, shipmentId);
    }

    private void verifyNotificationCreated(String shipmentId, NotificationType expectedType, String expectedLocationOrDestination) {
        await("Notification should be created for shipment: " + shipmentId)
                .atMost(NOTIFICATION_PROCESSING_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationService.findByShipmentId(shipmentId);

                    assertThat(notifications)
                            .as("Should have at least one notification for shipment %s", shipmentId)
                            .isNotEmpty();

                    boolean foundExpectedNotification = notifications.stream()
                            .anyMatch(n -> n.getType() == expectedType &&
                                         n.getMessage().contains(expectedLocationOrDestination));

                    assertThat(foundExpectedNotification)
                            .as("Should find notification of type %s containing %s", expectedType, expectedLocationOrDestination)
                            .isTrue();

                    logger.debug("Verified notification created: shipmentId={}, type={}, location/destination={}",
                            shipmentId, expectedType, expectedLocationOrDestination);
                });
    }
}