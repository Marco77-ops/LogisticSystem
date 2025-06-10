package com.luckypets.logistics.shipmentservice.integrationtest;

import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentService;
import com.luckypets.logistics.shipmentservice.service.ShipmentServiceImpl; // Import ShipmentServiceImpl
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ShipmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentService shipmentService; // Autowire the service

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        // Clear in-memory storage of the service for test isolation
        // Cast to ShipmentServiceImpl to access the specific test helper method
        if (shipmentService instanceof ShipmentServiceImpl) {
            ((ShipmentServiceImpl) shipmentService).clearInMemoryStorageForTests();
        }
    }

    @Test
    void createShipment_validRequest_returnsCreatedAndShipment() throws Exception {
        String requestJson = """
            {
                "origin": "Munich",
                "destination": "Berlin",
                "customerId": "CUST001"
            }
            """;

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").exists())
                .andExpect(jsonPath("$.origin").value("Munich"))
                .andExpect(jsonPath("$.destination").value("Berlin"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createShipment_invalidRequest_returnsBadRequest() throws Exception {
        String requestJson = """
            {
                "origin": "",
                "destination": "Berlin",
                "customerId": "CUST001"
            }
            """; // Origin is blank

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShipmentById_existingShipment_returnsShipment() throws Exception {
        // Create a shipment via the service to ensure it's in its internal map
        ShipmentRequest createRequest = new ShipmentRequest();
        createRequest.setOrigin("TestOrigin");
        createRequest.setDestination("TestDestination");
        createRequest.setCustomerId("TestCustomer");
        ShipmentEntity createdEntity = shipmentService.createShipment(createRequest);
        String createdShipmentId = createdEntity.getShipmentId();

        mockMvc.perform(get("/api/v1/shipments/{id}", createdShipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(createdShipmentId))
                .andExpect(jsonPath("$.origin").value("TestOrigin"));
    }

    @Test
    void getShipmentById_nonExistingShipment_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllShipments_returnsAllShipments() throws Exception {
        // Create some shipments directly via the service
        ShipmentRequest req1 = new ShipmentRequest();
        req1.setOrigin("Munich"); req1.setDestination("Berlin"); req1.setCustomerId("C1");
        shipmentService.createShipment(req1);

        ShipmentRequest req2 = new ShipmentRequest();
        req2.setOrigin("Frankfurt"); req2.setDestination("Hamburg"); req2.setCustomerId("C2");
        shipmentService.createShipment(req2);

        mockMvc.perform(get("/api/v1/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].destination").exists()) // Order not guaranteed for ConcurrentHashMap
                .andExpect(jsonPath("$[1].destination").exists());
    }

    @Test
    void deleteShipment_existing_returnsNoContent() throws Exception {
        // Create a shipment to delete
        ShipmentRequest createRequest = new ShipmentRequest();
        createRequest.setOrigin("DeleteOrigin");
        createRequest.setDestination("DeleteDestination");
        createRequest.setCustomerId("DeleteCustomer");
        ShipmentEntity createdEntity = shipmentService.createShipment(createRequest);
        String shipmentIdToDelete = createdEntity.getShipmentId();

        mockMvc.perform(delete("/api/v1/shipments/{id}", shipmentIdToDelete))
                .andExpect(status().isNoContent());

        // Verify it's actually gone from the in-memory storage
        assertThat(shipmentService.getShipmentById(shipmentIdToDelete)).isEmpty();
    }

    @Test
    void deleteShipment_notExisting_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/shipments/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }
}
