package com.luckypets.logistics.shipmentservice.integrationtest;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
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
class ShipmentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private KafkaConsumer<String, ShipmentCreatedEvent> kafkaConsumer;

    @BeforeEach
    void setupKafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-consumer-group");
        props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put("value.deserializer", JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentCreatedEvent.class.getName());
        props.put("auto.offset.reset", "earliest");

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("shipment-created"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        shipmentRepository.deleteAll();
    }

    @Test
    void createShipment_persistsEntity_and_publishesEventToKafka() throws Exception {
        String requestJson = """
            {
                "origin": "München",
                "destination": "Berlin",
                "customerId": "C111"
            }
        """;

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        List<ShipmentEntity> shipments = shipmentRepository.findAll();
        assertThat(shipments).hasSize(1);
        assertThat(shipments.get(0).getDestination()).isEqualTo("Berlin");

        ConsumerRecord<String, ShipmentCreatedEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000;

        poll:
        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, ShipmentCreatedEvent> record : records) {
                received = record;
                break poll;
            }
        }

        assertThat(received).isNotNull();
        assertThat(received.value().getDestination()).isEqualTo("Berlin");
        assertThat(received.value().getShipmentId()).isNotEmpty();
    }
}