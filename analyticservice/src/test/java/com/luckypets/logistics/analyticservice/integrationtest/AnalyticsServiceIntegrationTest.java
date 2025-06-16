package com.luckypets.logistics.analyticservice.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import com.luckypets.logistics.analyticservice.service.AnalyticsService;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceIntegrationTest.class);
    private static final int KAFKA_READY_TIMEOUT = 60;
    private static final int ANALYTICS_TIMEOUT = 30;

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withStartupTimeout(java.time.Duration.ofMinutes(2));

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    @Value("${kafka.topic.delivered:shipment-delivered}")
    private String deliveredTopic;

    private KafkaProducer<String, String> producer;
    private ObjectMapper objectMapper;
    private String testRunId;
    private static AdminClient adminClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Warte explizit darauf, dass der Container gestartet ist
        kafka.start();

        String bootstrapServers = kafka.getBootstrapServers();
        String uniqueAppId = "analytics-test-" + System.currentTimeMillis();

        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.streams.application-id", () -> uniqueAppId);

        // Einfache Test-Konfiguration
        registry.add("spring.kafka.streams.processing.guarantee", () -> "at_least_once");
        registry.add("spring.kafka.streams.commit-interval", () -> 1000);
        registry.add("spring.kafka.streams.cache-max-size-buffering", () -> 0);
        registry.add("spring.kafka.streams.auto-startup", () -> false); // WICHTIG: Manueller Start

        // Consumer Properties
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "analytics-test-consumer");

        // Topic konfigurieren
        registry.add("kafka.topic.delivered", () -> "shipment-delivered");

        logger.info("Configured Kafka with bootstrap-servers: {} and application-id: {}",
                bootstrapServers, uniqueAppId);
    }

    @BeforeAll
    void setupKafka() throws Exception {
        // Admin Client erstellen
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        adminClient = AdminClient.create(adminProps);

        // Topics erstellen
        createTopics();

        // Warte bis Topics bereit sind
        await("Topics should be created")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        Set<String> topics = adminClient.listTopics().names().get();
                        return topics.contains("shipment-delivered");
                    } catch (Exception e) {
                        logger.warn("Waiting for topics: {}", e.getMessage());
                        return false;
                    }
                });

        logger.info("Topics created successfully");
    }

    private void createTopics() throws Exception {
        List<NewTopic> topics = Arrays.asList(
                new NewTopic("shipment-delivered", 1, (short) 1),
                new NewTopic("delivery-analytics", 1, (short) 1)
        );

        try {
            adminClient.createTopics(topics).all().get(10, TimeUnit.SECONDS);
            logger.info("Topics created: shipment-delivered, delivery-analytics");
        } catch (Exception e) {
            // Topics existieren möglicherweise bereits
            logger.info("Topics may already exist: {}", e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testRunId = UUID.randomUUID().toString().substring(0, 8);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        setupProducer();

        // Kafka Streams manuell starten
        if (!factoryBean.isRunning()) {
            factoryBean.start();
            logger.info("Started Kafka Streams manually");
        }

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
        if (factoryBean != null && factoryBean.isRunning()) {
            factoryBean.stop();
        }
    }

    private void setupProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);

        producer = new KafkaProducer<>(props);
    }

    @Test
    @DisplayName("Should process delivery event and update analytics")
    void shouldProcessDeliveryEventAndUpdateAnalytics() throws Exception {
        // Arrange: Warte bis Kafka Streams läuft
        waitForKafkaStreamsToStart();

        LocalDateTime eventTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        String shipmentId = "SHIP-TEST-" + testRunId;
        String location = "TestLocation-" + testRunId;

        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                shipmentId, location, location, eventTime, "corr-" + testRunId);

        // Act: Event senden
        sendEventAndVerifyDelivery(event);

        // Assert: Analytics prüfen
        verifyAnalyticsUpdated(location, eventTime, 1);
    }

    @Test
    @DisplayName("Should aggregate multiple events for same location")
    void shouldAggregateMultipleEventsForSameLocation() throws Exception {
        waitForKafkaStreamsToStart();

        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 11, 0);
        String location = "MultiLocation-" + testRunId;

        // Act: Mehrere Events senden
        for (int i = 1; i <= 3; i++) {
            ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                    "SHIP-MULTI-" + i + "-" + testRunId,
                    location, location,
                    baseTime.plusMinutes(i),
                    "corr-multi-" + i + "-" + testRunId);

            sendEventAndVerifyDelivery(event);
            Thread.sleep(500); // Kleine Pause zwischen Events
        }

        // Assert: Aggregierte Daten prüfen
        verifyAnalyticsUpdated(location, baseTime, 3);
    }

    private void waitForKafkaStreamsToStart() {
        logger.info("Waiting for Kafka Streams to start...");

        await("Kafka Streams should start")
                .atMost(KAFKA_READY_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        KafkaStreams streams = factoryBean.getKafkaStreams();
                        if (streams == null) {
                            logger.debug("Kafka Streams instance is null");
                            return false;
                        }

                        KafkaStreams.State state = streams.state();
                        logger.debug("Kafka Streams state: {}", state);

                        return state == KafkaStreams.State.RUNNING ||
                                state == KafkaStreams.State.REBALANCING;
                    } catch (Exception e) {
                        logger.warn("Error checking Kafka Streams state: {}", e.getMessage());
                        return false;
                    }
                });

        // Zusätzliche Zeit für Stream Processing Setup
        try {
            Thread.sleep(5000); // Erhöht auf 5 Sekunden
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Kafka Streams", e);
        }

        logger.info("Kafka Streams is ready");
    }

    private void sendEventAndVerifyDelivery(ShipmentDeliveredEvent event)
            throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {

        String eventJson = objectMapper.writeValueAsString(event);
        logger.debug("Sending event: {}", event.getShipmentId());

        ProducerRecord<String, String> record = new ProducerRecord<>(
                deliveredTopic, event.getShipmentId(), eventJson);

        producer.send(record).get(10, TimeUnit.SECONDS);
        producer.flush();

        logger.debug("Event sent successfully: {}", event.getShipmentId());
    }

    private void verifyAnalyticsUpdated(String location, LocalDateTime eventTime, int expectedCount) {
        LocalDateTime queryFrom = eventTime.minusHours(1);
        LocalDateTime queryTo = eventTime.plusHours(1);

        await("Analytics should be updated with location: " + location)
                .atMost(ANALYTICS_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<DeliveryCount> analytics = analyticsService.getAllLocationAnalytics(queryFrom, queryTo);

                    logger.debug("Current analytics: {}", analytics);

                    boolean foundLocation = analytics.stream()
                            .anyMatch(dc -> location.equals(dc.getLocation()) && dc.getCount() >= expectedCount);

                    assertThat(foundLocation)
                            .as("Should find location '%s' with count >= %d", location, expectedCount)
                            .isTrue();
                });
    }
}