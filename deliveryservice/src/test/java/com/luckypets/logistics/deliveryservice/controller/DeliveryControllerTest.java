package com.luckypets.logistics.deliveryservice.controller;

import com.luckypets.logistics.deliveryservice.persistence.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentRepository repository;

    @Test
    void getAll_returnsListOfShipments() throws Exception {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId("S1");
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setLastScannedAt(LocalDateTime.now());

        when(repository.findAll()).thenReturn(List.of(shipment));

        mockMvc.perform(get("/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shipmentId").value("S1"))
                .andExpect(jsonPath("$[0].status").value("IN_TRANSIT"));
    }

    @Test
    void getById_existingShipment_returnsShipment() throws Exception {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId("S2");
        shipment.setStatus(ShipmentStatus.DELIVERED);

        when(repository.findById("S2")).thenReturn(Optional.of(shipment));

        mockMvc.perform(get("/deliveries/S2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value("S2"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void getById_unknownShipment_returnsBadRequest() throws Exception {
        when(repository.findById("S404")).thenReturn(Optional.empty());

        mockMvc.perform(get("/deliveries/S404"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Shipment nicht gefunden: S404"));
    }

    @Test
    void getStatus_existingShipment_returnsStatus() throws Exception {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId("S3");
        shipment.setStatus(ShipmentStatus.DELIVERED);

        when(repository.findById("S3")).thenReturn(Optional.of(shipment));

        mockMvc.perform(get("/deliveries/S3/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("DELIVERED"));
    }

    @Test
    void getStatus_unknownShipment_returnsUnknown() throws Exception {
        when(repository.findById("S999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/deliveries/S999/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unbekannt"));
    }
}
