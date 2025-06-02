package com.luckypets.logistics.scanservice.integrationtest;

import com.luckypets.logistics.scanservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ScanServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private KafkaConsumer<String, ShipmentScannedEvent> kafkaConsumer;

    @BeforeEach
    void setupKafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-consumer-group");
        props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put("value.deserializer", JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentScannedEvent.class.getName());
        props.put("auto.offset.reset", "earliest");

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("shipment-scanned"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        shipmentRepository.deleteAll();
    }

    @Test
    void scanShipment_persistsEntity_and_publishesEventToKafka() throws Exception {
        // Vorbedingung: Ein Shipment anlegen, falls nötig
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId("SHIP-123");
        shipment.setOrigin("München");
        shipment.setDestination("Berlin");
        shipment.setCustomerId("C111");
        shipment.setStatus(ShipmentStatus.CREATED);
        shipmentRepository.save(shipment);

        String requestJson = """
            {
                "shipmentId": "SHIP-123",
                "location": "WAREHOUSE_A"
            }
        """;

        // Annahme: POST /api/v1/scans löst das Event aus
        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Event auf Kafka prüfen
        ConsumerRecord<String, ShipmentScannedEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000;

        poll:
        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, ShipmentScannedEvent> record : records) {
                received = record;
                break poll;
            }
        }

        assertThat(received).isNotNull();
        assertThat(received.value().getShipmentId()).isEqualTo("SHIP-123");
        assertThat(received.value().getLocation()).isEqualTo("WAREHOUSE_A");
        // ... weitere Assertions
    }
}
