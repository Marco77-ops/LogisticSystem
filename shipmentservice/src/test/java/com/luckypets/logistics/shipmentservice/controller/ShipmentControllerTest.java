package com.luckypets.logistics.shipmentservice.controller;

import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentEventProducer producer;

    @Test
    @DisplayName("POST /shipments mit gültigem Ziel gibt 200 OK zurück")
    void createShipment_validRequest_returnsOk() throws Exception {
        // Stub: Event senden soll einfach nichts tun
        doNothing().when(producer).sendShipmentCreatedEvent(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/shipments")
                        .param("destination", "Berlin"))
                .andExpect(status().isOk())
                .andExpect(content().string("ShipmentCreatedEvent gesendet für Ziel: Berlin"));
    }

    @Test
    @DisplayName("POST /shipments ohne Ziel gibt 400 Bad Request zurück")
    void createShipment_missingDestination_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/shipments"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Zieladresse darf nicht leer sein."));
    }
}
