package com.luckypets.logistics.shipmentservice.integrationtest;

import com.luckypets.logistics.shipmentservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus; // Import des Status-Enums
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ShipmentControllerIntegrationTest {

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

    @BeforeEach
    void cleanDb() {
        shipmentRepository.deleteAll();
    }

    @Test
    void createShipment_WithValidInput_ReturnsCreatedShipment() throws Exception {
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

        // Verifiziere, dass Shipment in DB ist
        List<ShipmentEntity> shipments = shipmentRepository.findAll();
        assertThat(shipments).hasSize(1);
        assertThat(shipments.get(0).getDestination()).isEqualTo("Berlin");
    }

    @Test
    void createShipment_MissingDestination_ReturnsBadRequest() throws Exception {
        String requestJson = """
            {
                "origin": "München",
                "customerId": "C111"
            }
        """;

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShipments_ReturnsAllShipments() throws Exception {
        // Seed-Daten einfügen - KORRIGIERT mit Status
        ShipmentEntity s1 = createTestShipment("test-id-1", "München", "Berlin", "C1");
        shipmentRepository.save(s1);

        ShipmentEntity s2 = createTestShipment("test-id-2", "Frankfurt", "Hamburg", "C2");
        shipmentRepository.save(s2);

        mockMvc.perform(get("/api/v1/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].destination").value("Berlin"))
                .andExpect(jsonPath("$[1].destination").value("Hamburg"));
    }

    // Hilfsmethode um vollständige Test-Shipments zu erstellen
    private ShipmentEntity createTestShipment(String shipmentId, String origin, String destination, String customerId) {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId(shipmentId);
        shipment.setOrigin(origin);
        shipment.setDestination(destination);
        shipment.setCustomerId(customerId);
        shipment.setStatus(ShipmentStatus.CREATED); // Status explizit setzen
        shipment.setCreatedAt(LocalDateTime.now());
        return shipment;
    }
}