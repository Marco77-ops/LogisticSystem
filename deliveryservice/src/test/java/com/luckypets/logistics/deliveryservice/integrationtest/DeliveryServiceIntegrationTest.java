package com.luckypets.logistics.deliveryservice.integrationtest;

import com.luckypets.logistics.deliveryservice.model.ShipmentEntity; // Local POJO
import com.luckypets.logistics.deliveryservice.service.DeliveryService; // Service interface
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl; // Concrete service for casting to access helper methods
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent; // Added for kafkaConsumerScanned
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class DeliveryServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.topic.delivered", () -> "shipment-delivered");
        registry.add("kafka.topic.scanned", () -> "shipment-scanned");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryService deliveryService;

    private KafkaConsumer<String, ShipmentDeliveredEvent> kafkaConsumerDelivered;
    private KafkaConsumer<String, ShipmentScannedEvent> kafkaConsumerScanned; // Added for testing listener flow

    @BeforeEach
    void setup() {
        // Clear in-memory storage of the service for test isolation
        if (deliveryService instanceof DeliveryServiceImpl) {
            ((DeliveryServiceImpl) deliveryService).clearInMemoryStorageForTests();
        }

        // Setup Kafka Consumer for ShipmentDeliveredEvent
        Properties deliveredProps = new Properties();
        deliveredProps.put("bootstrap.servers", kafka.getBootstrapServers());
        deliveredProps.put("group.id", "test-delivered-consumer-group-" + UUID.randomUUID());
        deliveredProps.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        deliveredProps.put("value.deserializer", JsonDeserializer.class);
        deliveredProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.luckypets.logistics.shared.events"); // Trust shared package
        deliveredProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentDeliveredEvent.class.getName());
        deliveredProps.put("auto.offset.reset", "earliest");
        deliveredProps.put("enable.auto.commit", false);

        kafkaConsumerDelivered = new KafkaConsumer<>(deliveredProps);
        kafkaConsumerDelivered.subscribe(List.of("shipment-delivered"));

        // Setup Kafka Consumer for ShipmentScannedEvent (to ensure listener gets events during integration tests)
        Properties scannedProps = new Properties();
        scannedProps.put("bootstrap.servers", kafka.getBootstrapServers());
        scannedProps.put("group.id", "test-scanned-consumer-group-" + UUID.randomUUID());
        scannedProps.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        scannedProps.put("value.deserializer", JsonDeserializer.class);
        scannedProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.luckypets.logistics.shared.events");
        scannedProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentScannedEvent.class.getName());
        scannedProps.put("auto.offset.reset", "earliest");
        scannedProps.put("enable.auto.commit", false);

        kafkaConsumerScanned = new KafkaConsumer<>(scannedProps);
        kafkaConsumerScanned.subscribe(List.of("shipment-scanned"));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumerDelivered != null) {
            kafkaConsumerDelivered.close();
        }
        if (kafkaConsumerScanned != null) {
            kafkaConsumerScanned.close();
        }
        // Clear in-memory storage after each test for full isolation
        if (deliveryService instanceof DeliveryServiceImpl) {
            ((DeliveryServiceImpl) deliveryService).clearInMemoryStorageForTests();
        }
    }

    @Test
    @DisplayName("Should successfully mark shipment as delivered and publish event")
    void markAsDelivered_updatesInMemoryState_and_publishesEventToKafka() throws Exception {
        // Arrange: Prepare an initial shipment state in the in-memory service
        ShipmentEntity shipmentToDeliver = new ShipmentEntity(
                "SHIP-123", // shipmentId
                "OriginCity", // origin
                "Berlin",     // destination
                "CUST456",    // customerId
                LocalDateTime.now().minusDays(1), // createdAt
                ShipmentStatus.IN_TRANSIT // initial status
        );
        shipmentToDeliver.setLastLocation("Somewhere in Transit"); // Set an initial last location

        if (deliveryService instanceof DeliveryServiceImpl) {
            ((DeliveryServiceImpl) deliveryService).addShipmentForTest(shipmentToDeliver);
        }

        String requestJson = """
            {
                "location": "Berlin"
            }
        """;

        // Act: Perform POST on Delivery-Endpoint
        mockMvc.perform(post("/deliveries/SHIP-123/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value("SHIP-123"))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.location").value("Berlin"))
                .andExpect(jsonPath("$.deliveredAt").exists());

        // Assert: Verify in-memory state
        Optional<ShipmentEntity> updatedShipment = ((DeliveryServiceImpl) deliveryService).findShipmentEntityById("SHIP-123");
        assertThat(updatedShipment).isPresent();
        assertThat(updatedShipment.get().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(updatedShipment.get().getLastLocation()).isEqualTo("Berlin");
        assertThat(updatedShipment.get().getDeliveredAt()).isNotNull();

        // Assert: Verify Kafka event
        ConsumerRecord<String, ShipmentDeliveredEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000; // 10 seconds timeout
        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumerDelivered.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                received = records.iterator().next();
                break;
            }
        }
        assertThat(received).isNotNull();
        assertThat(received.value().getShipmentId()).isEqualTo("SHIP-123");
        assertThat(received.value().getLocation()).isEqualTo("Berlin");
        assertThat(received.value().getDestination()).isEqualTo("Berlin");
        assertThat(received.value().getDeliveredAt()).isNotNull();
        assertThat(received.value().getCorrelationId()).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 for non-existent shipment when marking as delivered")
    void markAsDelivered_nonExistentShipment_returnsBadRequest() throws Exception {
        // Arrange: Ensure no shipment with this ID exists in in-memory storage
        if (deliveryService instanceof DeliveryServiceImpl) {
            ((DeliveryServiceImpl) deliveryService).clearInMemoryStorageForTests();
        }

        String requestJson = """
            {
                "location": "Some Place"
            }
        """;

        // Act & Assert
        mockMvc.perform(post("/deliveries/NON_EXISTENT_SHIPMENT/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest()) // DeliveryService returns BAD_REQUEST with error message
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Shipment not found: NON_EXISTENT_SHIPMENT"));
    }

    @Test
    @DisplayName("Should return 400 for missing location when marking as delivered")
    void markAsDelivered_missingLocation_returnsBadRequest() throws Exception {
        // Arrange: Prepare an initial shipment state in the in-memory service
        ShipmentEntity shipmentToDeliver = new ShipmentEntity(
                "SHIP-456",
                "Origin",
                "Destination",
                "CUST789",
                LocalDateTime.now().minusDays(1),
                ShipmentStatus.IN_TRANSIT
        );
        if (deliveryService instanceof DeliveryServiceImpl) {
            ((DeliveryServiceImpl) deliveryService).addShipmentForTest(shipmentToDeliver);
        }

        String requestJson = """
            {
                "location": ""
            }
        """; // Empty location

        // Act & Assert
        mockMvc.perform(post("/deliveries/SHIP-456/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("location must not be null or empty"));
    }

    @Test
    @DisplayName("Should return all shipments correctly")
    void getAllShipments_returnsAllShipments() throws Exception {
        // Arrange: Add some shipments to the in-memory store
        ShipmentEntity s1 = new ShipmentEntity("SHIP-001", "A", "B", "C1", LocalDateTime.now(), ShipmentStatus.CREATED);
        ShipmentEntity s2 = new ShipmentEntity("SHIP-002", "X", "Y", "C2", LocalDateTime.now(), ShipmentStatus.IN_TRANSIT);
        ((DeliveryServiceImpl) deliveryService).addShipmentForTest(s1);
        ((DeliveryServiceImpl) deliveryService).addShipmentForTest(s2);

        // Act & Assert
        mockMvc.perform(get("/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shipmentId").exists())
                .andExpect(jsonPath("$[1].shipmentId").exists());
    }

    @Test
    @DisplayName("Should return shipment by ID correctly")
    void getShipmentById_returnsShipment_whenExists() throws Exception {
        // Arrange: Add a shipment to the in-memory store
        ShipmentEntity s1 = new ShipmentEntity("SHIP-ID-TEST", "Origin", "Dest", "Cust", LocalDateTime.now(), ShipmentStatus.CREATED);
        ((DeliveryServiceImpl) deliveryService).addShipmentForTest(s1);

        // Act & Assert
        mockMvc.perform(get("/deliveries/SHIP-ID-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value("SHIP-ID-TEST"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Should return status correctly")
    void getShipmentStatus_returnsStatus() throws Exception {
        // Arrange: Add a shipment to the in-memory store
        ShipmentEntity s1 = new ShipmentEntity("STATUS-TEST", "Origin", "Dest", "Cust", LocalDateTime.now(), ShipmentStatus.IN_TRANSIT);
        ((DeliveryServiceImpl) deliveryService).addShipmentForTest(s1);

        // Act & Assert
        mockMvc.perform(get("/deliveries/STATUS-TEST/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("IN_TRANSIT"));
    }
}
