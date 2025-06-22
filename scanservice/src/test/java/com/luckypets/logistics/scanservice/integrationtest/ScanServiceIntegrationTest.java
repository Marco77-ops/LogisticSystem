package com.luckypets.logistics.scanservice.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckypets.logistics.scanservice.ScanServiceApplication;
import com.luckypets.logistics.scanservice.model.ScanRequest;
import com.luckypets.logistics.scanservice.model.ShipmentEntity;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ScanServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"shipment-created", "shipment-scanned"}, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@ActiveProfiles("test")
@DirtiesContext
public class ScanServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ScanServiceIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScanServiceImpl scanService;

    // **Korrekt: Feld-Injektion, KEIN Konstruktor!**
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Consumer<String, ShipmentCreatedEvent> createdEventConsumer;
    private Consumer<String, ShipmentScannedEvent> scannedEventConsumer;

    @BeforeEach
    void setUp() {
        log.info("Setting up Kafka consumers for integration tests. Bootstrap Servers: {}", bootstrapServers);

        scanService.clearInMemoryStorageForTests();

        // Consumer für ShipmentCreatedEvent
        Map<String, Object> createdConsumerProps = KafkaTestUtils.consumerProps("test-group-created", "true", embeddedKafkaBroker);
        createdConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        createdConsumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.luckypets.logistics.shared.events");
        createdEventConsumer = new DefaultKafkaConsumerFactory<String, ShipmentCreatedEvent>(
                createdConsumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentCreatedEvent.class)
        ).createConsumer();
        createdEventConsumer.subscribe(Collections.singleton("shipment-created"));
        KafkaTestUtils.getRecords(createdEventConsumer, Duration.ofMillis(100)); // partition assignment workaround

        // Consumer für ShipmentScannedEvent
        Map<String, Object> scannedConsumerProps = KafkaTestUtils.consumerProps("test-group-scanned", "true", embeddedKafkaBroker);
        scannedConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        scannedConsumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.luckypets.logistics.shared.events");
        scannedEventConsumer = new DefaultKafkaConsumerFactory<String, ShipmentScannedEvent>(
                scannedConsumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentScannedEvent.class)
        ).createConsumer();
        scannedEventConsumer.subscribe(Collections.singleton("shipment-scanned"));
        KafkaTestUtils.getRecords(scannedEventConsumer, Duration.ofMillis(100)); // partition assignment workaround

        log.info("Kafka setup complete.");
    }

    @AfterEach
    void tearDown() {
        if (createdEventConsumer != null) createdEventConsumer.close();
        if (scannedEventConsumer != null) scannedEventConsumer.close();
    }

    @Test
    void scanShipment_existingShipment_updatesAndPublishesEvent() throws Exception {
        // Arrange: Shipment direkt im In-Memory-Store anlegen
        String shipmentId = "SHIP-123";
        String origin = "TestOrigin";
        String destination = "TestDestination";
        String customerId = "test-customer";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS);

        ShipmentEntity initialShipment = new ShipmentEntity(shipmentId, origin, destination, customerId, createdAt, ShipmentStatus.CREATED);
        scanService.addShipmentForTest(initialShipment);
        log.info("Initial shipment added to in-memory store: {}", initialShipment);

        // Act: Scan-Request
        String scanLocation = "WAREHOUSE_A";
        ScanRequest scanRequest = new ScanRequest();
        scanRequest.setShipmentId(shipmentId);
        scanRequest.setLocation(scanLocation);
        String requestBody = objectMapper.writeValueAsString(scanRequest);

        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("Shipment successfully scanned at " + scanLocation)));
        log.info("Scan request sent and 201 Created received.");

        // Assert: In-Memory-Status
        Optional<ShipmentEntity> updatedShipment = scanService.findById(shipmentId);
        assertThat(updatedShipment).isPresent();
        assertThat(updatedShipment.get().getLastLocation()).isEqualTo(scanLocation);
        assertThat(updatedShipment.get().getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(updatedShipment.get().getLastScannedAt()).isNotNull();

        // Assert: Kafka Event publiziert
        ConsumerRecords<String, ShipmentScannedEvent> records = KafkaTestUtils.getRecords(scannedEventConsumer, Duration.ofSeconds(10));
        assertThat(records.count()).isEqualTo(1);
        ShipmentScannedEvent publishedEvent = records.iterator().next().value();

        assertThat(publishedEvent.getShipmentId()).isEqualTo(shipmentId);
        assertThat(publishedEvent.getLocation()).isEqualTo(scanLocation);
        assertThat(publishedEvent.getScannedAt()).isNotNull();
        assertThat(publishedEvent.getDestination()).isEqualTo(destination);
        assertThat(publishedEvent.getCorrelationId()).isNotNull().isNotBlank();
        log.info("ShipmentScannedEvent published to Kafka and verified: {}", publishedEvent);
    }

    @Test
    void scanShipment_nonExistentShipment_returnsBadRequest() throws Exception {
        String shipmentId = "NON_EXISTENT_SHIPMENT";
        String scanLocation = "WAREHOUSE_B";
        ScanRequest scanRequest = new ScanRequest();
        scanRequest.setShipmentId(shipmentId);
        scanRequest.setLocation(scanLocation);
        String requestBody = objectMapper.writeValueAsString(scanRequest);

        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Shipment with ID " + shipmentId + " not found.")));
        log.info("Scan request for non-existent shipment returned 400 Bad Request as expected.");
    }

    @Test
    void scanShipment_invalidRequest_returnsBadRequest() throws Exception {
        // Missing shipmentId
        String invalidRequestBody = "{\"location\": \"WAREHOUSE_C\"}";

        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Shipment ID cannot be null")));
        log.info("Scan request with invalid body returned 400 Bad Request as expected (missing shipmentId).");

        // Missing location
        String invalidRequestBody2 = "{\"shipmentId\": \"SHIP-456\"}";
        mockMvc.perform(post("/api/v1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody2))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Location cannot be null")));
        log.info("Scan request with invalid body returned 400 Bad Request as expected (missing location).");
    }
}
