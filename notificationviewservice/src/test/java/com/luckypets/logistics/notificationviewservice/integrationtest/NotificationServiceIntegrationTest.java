package com.luckypets.logistics.notificationviewservice.integrationtest;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer; // Added import
import org.apache.kafka.clients.producer.ProducerRecord; // Added import
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer; // Added import
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationServiceIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        kafka.start();
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);

        // Optional: Testdatenbank für H2, falls benötigt
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private KafkaConsumer<String, ShipmentDeliveredEvent> kafkaConsumer;

    @BeforeEach
    void setupKafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentDeliveredEvent.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("shipment-delivered"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    void shouldPublishShipmentDeliveredEventToKafka() {
        // Beispiel-Event
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-123", "Berlin", "Berlin", LocalDateTime.now(), "corr-id-001"
        );

        // PRODUCER: Send the event to Kafka (UNCOMMENTED)
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        producerProps.put("key.serializer", org.apache.kafka.common.serialization.StringSerializer.class);
        producerProps.put("value.serializer", JsonSerializer.class.getName()); // Use JsonSerializer

        try (KafkaProducer<String, ShipmentDeliveredEvent> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>("shipment-delivered", "SHIP-123", event));
            producer.flush();
        }

        // Warten auf das Event im Topic
        ConsumerRecord<String, ShipmentDeliveredEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000;
        poll:
        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, ShipmentDeliveredEvent> record : records) {
                received = record;
                break poll;
            }
        }

        assertThat(received).isNotNull();
        assertThat(received.value().getShipmentId()).isEqualTo("SHIP-123");
        assertThat(received.value().getLocation()).isEqualTo("Berlin");
        assertThat(received.value().getCorrelationId()).isNotNull();
    }
}