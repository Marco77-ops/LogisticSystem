package com.luckypets.logistics.scanservice.controller;

import com.luckypets.logistics.scanservice.model.Shipment;
import com.luckypets.logistics.scanservice.repository.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScanController.class)
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    @MockBean
    private ShipmentRepository repository;

    @Test
    @DisplayName("POST /scans sendet ShipmentScannedEvent bei gültigem Shipment")
    void scanShipment_validShipment_sendsKafkaEvent() throws Exception {
        // Arrange
        Shipment mockShipment = new Shipment("123", "Berlin", LocalDateTime.now());
        when(repository.findById("123")).thenReturn(Optional.of(mockShipment));

        // Act
        mockMvc.perform(post("/scans")
                        .param("shipmentId", "123")
                        .param("location", "Leipzig"))
                .andExpect(status().isOk())
                .andExpect(content().string("ShipmentScannedEvent gesendet für ShipmentId: 123"));

        // Assert
        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("shipment-scanned"), eq("123"), eventCaptor.capture());

        ShipmentScannedEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getShipmentId()).isEqualTo("123");
        assertThat(sentEvent.getLocation()).isEqualTo("Leipzig");
        assertThat(sentEvent.getDestination()).isEqualTo("Berlin");
        assertThat(sentEvent.getScannedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /scans mit unbekannter shipmentId gibt 400 zurück")
    void scanShipment_unknownShipment_returnsBadRequest() throws Exception {
        when(repository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(post("/scans")
                        .param("shipmentId", "999")
                        .param("location", "Hamburg"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Shipment mit ID 999 nicht gefunden"));
    }
}
