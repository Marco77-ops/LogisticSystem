package com.luckypets.logistics.deliveryservice.integrationtest;

import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DeliveryServiceIntegrationTest {

    @MockBean
    private com.luckypets.logistics.deliveryservice.listener.ShipmentScannedListener shipmentScannedListener;

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);

        // Disable auto-startup of Kafka listeners during test
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> "false");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        // Configure test database
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private KafkaConsumer<String, ShipmentDeliveredEvent> kafkaConsumer;

    @BeforeEach
    void setupKafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-consumer-group-" + UUID.randomUUID());
        props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put("value.deserializer", JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentDeliveredEvent.class.getName());
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", false);

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("shipment-delivered"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        shipmentRepository.deleteAll();
    }

    @Test
    void markAsDelivered_persistsEntity_and_publishesEventToKafka() throws Exception {
        // Testdaten in H2 anlegen
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId("SHIP-123");
        shipment.setOrigin("Munich");
        shipment.setDestination("Berlin");
        shipment.setCustomerId("C111");
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setLastLocation("WAREHOUSE_A");
        shipment.setCreatedAt(LocalDateTime.now().minusDays(1));
        shipmentRepository.save(shipment);

        String requestJson = """
            {
                "location": "Berlin"
            }
        """;

        // POST auf Delivery-Endpoint
        mockMvc.perform(post("/deliveries/SHIP-123/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value("SHIP-123"))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.success").value(true));

        // DB prüfen
        Optional<ShipmentEntity> updatedShipment = shipmentRepository.findById("SHIP-123");
        assertThat(updatedShipment).isPresent();
        assertThat(updatedShipment.get().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(updatedShipment.get().getLastLocation()).isEqualTo("Berlin");
        assertThat(updatedShipment.get().getDeliveredAt()).isNotNull();

        // Kafka prüfen
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