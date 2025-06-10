package com.luckypets.logistics.scanservice.integrationtest;

import com.luckypets.logistics.scanservice.service.ScanService;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.scanservice.model.ShipmentEntity; // Import local ShipmentEntity
import com.luckypets.logistics.shared.model.ShipmentStatus; // Import shared ShipmentStatus
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;
import java.util.Optional; // Import Optional
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // Import content for error messages


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScanServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // For testing REST endpoints

    @Autowired
    private ScanService scanService; // For direct service interaction, especially for setup/teardown

    private KafkaConsumer<String, ShipmentScannedEvent> kafkaConsumerScanned;
    private KafkaConsumer<String, ShipmentCreatedEvent> kafkaConsumerCreated; // For consuming created events (optional, if you need to test the listener flow)

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        // Clear in-memory storage of the service for test isolation
        if (scanService instanceof ScanServiceImpl) {
            ((ScanServiceImpl) scanService).clearInMemoryStorageForTests();
        }

        // Setup Kafka Consumer for ShipmentScannedEvent
        Properties propsScanned = new Properties();
        propsScanned.put("bootstrap.servers", kafka.getBootstrapServers());
        propsScanned.put("group.id", "test-group-scanned-" + UUID.randomUUID()); // Unique group id
        propsScanned.put("auto.offset.reset", "earliest");

        kafkaConsumerScanned = new KafkaConsumer<>(
                propsScanned,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentScannedEvent.class, false)
        );
        kafkaConsumerScanned.subscribe(Collections.singletonList("shipment-scanned"));

        // Setup Kafka Consumer for ShipmentCreatedEvent (optional, only if you test the listener logic)
        Properties propsCreated = new Properties();
        propsCreated.put("bootstrap.servers", kafka.getBootstrapServers());
        propsCreated.put("group.id", "test-group-created-" + UUID.randomUUID()); // Unique group id
        propsCreated.put("auto.offset.reset", "earliest");

        kafkaConsumerCreated = new KafkaConsumer<>(
                propsCreated,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentCreatedEvent.class, false)
        );
        kafkaConsumerCreated.subscribe(Collections.singletonList("shipment-created"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumerScanned != null) {
            kafkaConsumerScanned.close();
        }
        if (kafkaConsumerCreated != null) {
            kafkaConsumerCreated.close();
        }
    }

    @Test
    void scanShipment_existingShipment_updatesAndPublishesEvent() throws Exception {
        // Arrange: Populate the ScanService's in-memory store directly for testing
        ShipmentEntity shipmentToScan = new ShipmentEntity();
        shipmentToScan.setShipmentId("SHIP-123");
        shipmentToScan.setOrigin("München");
        shipmentToScan.setDestination("Berlin");
        shipmentToScan.setCustomerId("C111");
        shipmentToScan.setStatus(ShipmentStatus.CREATED); // Initial status
        shipmentToScan.setCreatedAt(LocalDateTime.now());
        shipmentToScan.setLastLocation("Initial location from creation"); // Set an initial last location

        if (scanService instanceof ScanServiceImpl) {
            ((ScanServiceImpl) scanService).addShipmentForTest(shipmentToScan);
        }

        String requestJson = """
            {
                "shipmentId": "SHIP-123",
                "location": "WAREHOUSE_A"
            }
        """;

        // Act (via controller)
        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Assert: Verify internal state changed and Kafka event was sent
        // Verify internal state (optional, but good for in-memory service)
        Optional<ShipmentEntity> updatedShipmentOpt = ((ScanServiceImpl) scanService).findById("SHIP-123");
        assertTrue(updatedShipmentOpt.isPresent());
        assertThat(updatedShipmentOpt.get().getLastLocation()).isEqualTo("WAREHOUSE_A");
        assertThat(updatedShipmentOpt.get().getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);


        // Verify Kafka event
        ConsumerRecord<String, ShipmentScannedEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumerScanned.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                received = records.iterator().next();
                break;
            }
        }
        assertThat(received).isNotNull();
        assertThat(received.value().getShipmentId()).isEqualTo("SHIP-123");
        assertThat(received.value().getLocation()).isEqualTo("WAREHOUSE_A");
        assertThat(received.value().getDestination()).isEqualTo("Berlin"); // Destination comes from the in-memory entity
    }

    @Test
    void scanShipment_nonExistingShipment_returnsFailure() throws Exception {
        // Arrange (no existing shipment in the service's in-memory map)
        String requestJson = """
            {
                "shipmentId": "NON_EXISTENT",
                "location": "Warehouse B"
            }
        """;

        // Act (via controller)
        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Shipment not found")); // Verify the error message
    }

    @Test
    void scanShipment_nullShipmentId_returnsBadRequest() throws Exception {
        String requestJson = """
            {
                "shipmentId": null,
                "location": "Location C"
            }
        """;

        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Shipment ID cannot be null")); // Verify the error message
    }

    @Test
    void scanShipment_emptyLocation_returnsBadRequest() throws Exception {
        String requestJson = """
            {
                "shipmentId": "SHIP456",
                "location": ""
            }
        """;

        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Location cannot be empty")); // Verify the error message
    }
}
