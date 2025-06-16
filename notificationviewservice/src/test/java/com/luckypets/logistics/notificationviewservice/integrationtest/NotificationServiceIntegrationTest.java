package com.luckypets.logistics.notificationviewservice.integrationtest;

import com.luckypets.logistics.notificationviewservice.listener.ShipmentEventListener;
import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {com.luckypets.logistics.notificationviewservice.NotificationViewServiceApplication.class}
)
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@ActiveProfiles("test")
@Testcontainers
class NotificationServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ShipmentEventListener shipmentEventListener;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    private KafkaConsumer<String, ShipmentDeliveredEvent> kafkaConsumer;

    @BeforeEach
    void setUp() {
        // Setup Kafka Consumer for integration tests (following ShipmentServiceIntegrationTest pattern)
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-group-" + System.currentTimeMillis()); // Unique group id for each test
        props.put("auto.offset.reset", "earliest");

        kafkaConsumer = new KafkaConsumer<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentDeliveredEvent.class, false)
        );
        kafkaConsumer.subscribe(Collections.singletonList("shipment-delivered"));

        // Clear any existing notifications
        notificationService.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    void shouldPublishShipmentDeliveredEventToKafka() {
        // Create test event
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-123", "Berlin", "Berlin", LocalDateTime.now(), "corr-id-001"
        );

        // Send event using simple producer (following ShipmentServiceIntegrationTest pattern)
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafka.getBootstrapServers());

        try (KafkaProducer<String, ShipmentDeliveredEvent> producer = new KafkaProducer<>(
                producerProps,
                new StringSerializer(),
                new JsonSerializer<>()
        )) {
            producer.send(new ProducerRecord<>("shipment-delivered", "SHIP-123", event));
            producer.flush();
        }

        // Wait for the event (following ShipmentServiceIntegrationTest pattern)
        ConsumerRecord<String, ShipmentDeliveredEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000; // 10 seconds timeout

        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                received = records.iterator().next(); // Get the first record
                break;
            }
        }

        assertThat(received).isNotNull();
        assertThat(received.value().getShipmentId()).isEqualTo("SHIP-123");
        assertThat(received.value().getLocation()).isEqualTo("Berlin");
        assertThat(received.value().getCorrelationId()).isNotNull();
    }

    @Test
    void shouldCreateNotificationWhenShipmentDeliveredEventIsProcessed() {
        // Create a test event
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-456", "Munich", "Munich", LocalDateTime.now(), "corr-id-002"
        );

        // Directly call the event handler method to simulate Kafka message consumption
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        shipmentEventListener.handleShipmentDelivered(
                event,
                "shipment-delivered",
                0,
                0L,
                acknowledgment
        );

        // Verify that a notification was created with the correct properties
        List<Notification> notifications = notificationService.findByShipmentId("SHIP-456");
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SHIPMENT_DELIVERED);
        assertThat(notifications.get(0).getMessage()).contains("has been delivered to its destination Munich");
    }
}