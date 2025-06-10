package com.luckypets.logistics.analyticservice.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import com.luckypets.logistics.analyticservice.service.StateStoreQueryService;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class AnalyticsServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private StateStoreQueryService stateStoreQueryService;

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.streams.application-id", () -> "analytics-test-app");
        registry.add("spring.kafka.streams.properties.commit.interval.ms", () -> "5000");
        registry.add("spring.kafka.streams.properties.producer.transaction.timeout.ms", () -> "10000");
        registry.add("kafka.topic.delivered", () -> "shipment-delivered");
        registry.add("kafka.streams.state-store-name", () -> "delivery-counts-store");
        registry.add("kafka.streams.window.size-minutes", () -> 60);
        registry.add("kafka.streams.window.grace-period-minutes", () -> 1);
    }

    @BeforeEach
    void setupTopicsAndForceRestartKafkaStreams() throws Exception {
        System.out.println("=== Setting up test environment ===");

        // Step 1: Stop any existing Kafka Streams instance
        System.out.println("Stopping any existing Kafka Streams...");
        try {
            factoryBean.stop();
            Thread.sleep(2000); // Give it time to stop completely
        } catch (Exception e) {
            System.out.println("No existing Kafka Streams to stop: " + e.getMessage());
        }

        // Step 2: Create topics
        System.out.println("Creating Kafka topics...");
        createRequiredTopics();

        // Step 3: Start fresh Kafka Streams instance
        System.out.println("Starting fresh Kafka Streams...");
        factoryBean.start();

        // Step 4: Wait for Kafka Streams to be running
        System.out.println("Waiting for Kafka Streams to be ready...");
        await().atMost(90, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .until(() -> {
                    KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
                    if (kafkaStreams == null) {
                        System.out.println("Kafka Streams is null, waiting...");
                        return false;
                    }

                    KafkaStreams.State state = kafkaStreams.state();
                    System.out.println("Kafka Streams state: " + state);

                    if (state == KafkaStreams.State.ERROR) {
                        System.err.println("Kafka Streams is in ERROR state - this should not happen!");
                        return false;
                    }

                    return state.isRunningOrRebalancing();
                });

        System.out.println("Test environment ready!");
    }

    private void createRequiredTopics() throws Exception {
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            NewTopic deliveredTopic = new NewTopic("shipment-delivered", 1, (short) 1);
            CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(deliveredTopic));
            result.all().get(10, TimeUnit.SECONDS);
            System.out.println("Topic 'shipment-delivered' created successfully");
        }

        // Wait for metadata propagation
        Thread.sleep(5000);
    }

    @Test
    @DisplayName("Should process delivered events and update state store")
    void shouldProcessDeliveredEvents_and_updateStateStore() throws Exception {
        System.out.println("=== Starting test execution ===");

        // Additional wait to ensure state store is fully ready
        Thread.sleep(8000);

        // Create test producer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        LocalDateTime eventTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(1).plusMinutes(30);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                    "SHIP-TEST-123",
                    "Berlin",
                    "Berlin",
                    eventTime,
                    "test-correlation-id-1"
            );

            String eventJson = mapper.writeValueAsString(event);
            System.out.println("Sending event: " + eventJson);

            producer.send(new ProducerRecord<>("shipment-delivered", event.getShipmentId(), eventJson)).get();
            producer.flush();
            System.out.println("Event sent successfully");

            LocalDateTime windowStartForEvent = eventTime.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime queryFrom = windowStartForEvent.minusMinutes(1);
            LocalDateTime queryTo = windowStartForEvent.plusHours(1).plusMinutes(1);

            System.out.println("Querying for events between " + queryFrom + " and " + queryTo);

            await().atMost(45, TimeUnit.SECONDS)
                    .pollInterval(3, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        System.out.println("Checking state store...");
                        List<DeliveryCount> berlinCounts = stateStoreQueryService.getDeliveryCountsForLocation("Berlin", queryFrom, queryTo);

                        System.out.println("Found " + berlinCounts.size() + " delivery count records");
                        berlinCounts.forEach(dc -> System.out.println("  -> " + dc));

                        assertThat(berlinCounts).isNotEmpty();

                        Optional<DeliveryCount> relevantCount = berlinCounts.stream()
                                .filter(dc -> dc.getWindowStart() != null && dc.getWindowStart().equals(windowStartForEvent))
                                .findFirst();

                        assertThat(relevantCount).isPresent();
                        assertThat(relevantCount.get().getLocation()).isEqualTo("Berlin");
                        assertThat(relevantCount.get().getCount()).isEqualTo(1L);
                    });

            System.out.println("=== Test completed successfully ===");
        }
    }
}