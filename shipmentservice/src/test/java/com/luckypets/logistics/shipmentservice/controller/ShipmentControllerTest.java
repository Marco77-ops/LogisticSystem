package com.luckypets.logistics.shipmentservice.controller;

import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentStatus; // Import für Enum
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
public class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService; // Service wird gemockt

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createShipment_shouldReturnCreatedShipment() throws Exception {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Warehouse A");
        request.setDestination("Customer B");
        request.setCustomerId("CUST001");

        ShipmentEntity createdShipment = new ShipmentEntity();
        createdShipment.setShipmentId("S123");
        createdShipment.setOrigin(request.getOrigin());
        createdShipment.setDestination(request.getDestination());
        createdShipment.setCustomerId(request.getCustomerId());
        createdShipment.setStatus(ShipmentStatus.CREATED); // Verwendung des Enums direkt
        createdShipment.setCreatedAt(LocalDateTime.now());

        when(shipmentService.createShipment(any(ShipmentRequest.class))).thenReturn(createdShipment);

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId", is("S123")))
                .andExpect(jsonPath("$.status", is("CREATED"))) // Erwartet Enum-Namen oder dessen toString()
                .andExpect(jsonPath("$.origin", is("Warehouse A")));
    }

    @Test
    void getShipmentById_whenShipmentExists_shouldReturnShipment() throws Exception {
        String shipmentId = "S456";
        ShipmentEntity mockShipment = new ShipmentEntity();
        mockShipment.setShipmentId(shipmentId);
        mockShipment.setDestination("Berlin");
        mockShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        mockShipment.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(shipmentService.getShipmentById(shipmentId)).thenReturn(Optional.of(mockShipment));

        mockMvc.perform(get("/api/v1/shipments/{id}", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId", is(shipmentId)))
                .andExpect(jsonPath("$.destination", is("Berlin")))
                .andExpect(jsonPath("$.status", is("IN_TRANSIT"))); // Erwartet Enum-Namen
    }

    @Test
    void getShipmentById_whenShipmentNotExists_shouldReturnNotFound() throws Exception {
        String shipmentId = "S_NON_EXISTENT";
        when(shipmentService.getShipmentById(shipmentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/shipments/{id}", shipmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllShipments_shouldReturnShipmentList() throws Exception {
        ShipmentEntity shipment1 = new ShipmentEntity();
        shipment1.setShipmentId("S1");
        shipment1.setOrigin("Hamburg");
        shipment1.setStatus(ShipmentStatus.CREATED);

        ShipmentEntity shipment2 = new ShipmentEntity();
        shipment2.setShipmentId("S2");
        shipment2.setOrigin("Munich");
        shipment2.setStatus(ShipmentStatus.DELIVERED);

        List<ShipmentEntity> allShipments = Arrays.asList(shipment1, shipment2);

        when(shipmentService.getAllShipments()).thenReturn(allShipments);

        mockMvc.perform(get("/api/v1/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].shipmentId", is("S1")))
                .andExpect(jsonPath("$[0].status", is("CREATED"))) // Enum-Name
                .andExpect(jsonPath("$[1].shipmentId", is("S2")))
                .andExpect(jsonPath("$[1].status", is("DELIVERED"))); // Enum-Name
    }

    @Test
    void deleteShipment_whenShipmentExists_shouldReturnNoContent() throws Exception {
        String shipmentId = "S789";
        when(shipmentService.deleteShipment(shipmentId)).thenReturn(true); // Service gibt true zurück

        mockMvc.perform(delete("/api/v1/shipments/{id}", shipmentId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteShipment_whenShipmentNotExists_shouldReturnNotFound() throws Exception {
        String shipmentId = "S_NON_EXISTENT_DELETE";
        when(shipmentService.deleteShipment(shipmentId)).thenReturn(false); // Service gibt false zurück

        mockMvc.perform(delete("/api/v1/shipments/{id}", shipmentId))
                .andExpect(status().isNotFound());
    }
}
