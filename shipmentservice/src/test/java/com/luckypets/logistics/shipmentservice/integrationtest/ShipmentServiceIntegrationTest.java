package com.luckypets.logistics.shipmentservice.integrationtest;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentService;
import com.luckypets.logistics.shipmentservice.service.ShipmentServiceImpl; // Import ShipmentServiceImpl
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ShipmentServiceIntegrationTest {

    @Autowired
    private ShipmentService shipmentService; // Autowire the actual service

    private KafkaConsumer<String, ShipmentCreatedEvent> kafkaConsumer;

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        // Setup Kafka Consumer for integration tests
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-group-" + System.currentTimeMillis()); // Unique group id for each test
        props.put("auto.offset.reset", "earliest");

        kafkaConsumer = new KafkaConsumer<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentCreatedEvent.class, false)
        );
        kafkaConsumer.subscribe(Collections.singletonList("shipment-created"));

        // Clear in-memory storage of the service for test isolation
        // Cast to ShipmentServiceImpl to access the specific test helper method
        if (shipmentService instanceof ShipmentServiceImpl) {
            ((ShipmentServiceImpl) shipmentService).clearInMemoryStorageForTests();
        }
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    void createShipment_persistsEntityInMemory_and_publishesEventToKafka() throws Exception {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("München");
        request.setDestination("Berlin");
        request.setCustomerId("C111");

        ShipmentEntity createdShipment = shipmentService.createShipment(request);

        // Verify entity is in-memory
        assertThat(createdShipment).isNotNull();
        assertThat(shipmentService.getShipmentById(createdShipment.getShipmentId())).isPresent();
        assertThat(shipmentService.getShipmentById(createdShipment.getShipmentId()).get().getDestination()).isEqualTo("Berlin");


        // Verify Kafka event
        ConsumerRecord<String, ShipmentCreatedEvent> received = null;
        long timeout = System.currentTimeMillis() + 10_000; // 10 seconds timeout

        while (System.currentTimeMillis() < timeout) {
            var records = kafkaConsumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                received = records.iterator().next(); // Get the first record
                break;
            }
        }

        assertThat(received).isNotNull();
        assertThat(received.value().getDestination()).isEqualTo("Berlin");
        assertThat(received.value().getShipmentId()).isEqualTo(createdShipment.getShipmentId());
    }

    @Test
    void getShipmentById_existingShipment_returnsShipment() {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("OriginTest");
        request.setDestination("DestinationTest");
        request.setCustomerId("CustTest");
        ShipmentEntity created = shipmentService.createShipment(request);

        assertThat(shipmentService.getShipmentById(created.getShipmentId())).isPresent();
        assertThat(shipmentService.getShipmentById(created.getShipmentId()).get().getOrigin()).isEqualTo("OriginTest");
    }

    @Test
    void getShipmentById_nonExistingShipment_returnsEmpty() {
        assertThat(shipmentService.getShipmentById("non-existent")).isEmpty();
    }

    @Test
    void getAllShipments_returnsAllShipments() {
        shipmentService.createShipment(new ShipmentRequest() {{ setOrigin("O1"); setDestination("D1"); setCustomerId("C1"); }});
        shipmentService.createShipment(new ShipmentRequest() {{ setOrigin("O2"); setDestination("D2"); setCustomerId("C2"); }});

        assertThat(shipmentService.getAllShipments()).hasSize(2);
    }

    @Test
    void deleteShipment_existing_returnsTrueAndRemoves() {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("OriginDel");
        request.setDestination("DestinationDel");
        request.setCustomerId("CustDel");
        ShipmentEntity created = shipmentService.createShipment(request);

        assertThat(shipmentService.deleteShipment(created.getShipmentId())).isTrue();
        assertThat(shipmentService.getShipmentById(created.getShipmentId())).isEmpty();
    }

    @Test
    void deleteShipment_nonExisting_returnsFalse() {
        assertThat(shipmentService.deleteShipment("non-existent")).isFalse();
    }
}
