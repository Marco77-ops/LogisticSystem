package com.luckypets.logistics.scanservice.unittest;

import com.luckypets.logistics.scanservice.persistence.ShipmentRepository;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanService Creation Event Unit Tests")
class ScanServiceCreationEventTest { // ← Test-Endung

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    @InjectMocks
    private ScanServiceImpl scanService;

    private ShipmentEntity shipment1;
    private ShipmentEntity shipment2;

    @BeforeEach
    void setUp() {
        shipment1 = new ShipmentEntity();
        shipment1.setShipmentId("SHIP-001");
        shipment1.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment1.setLastLocation("ORIGIN");
        shipment1.setDestination("Berlin, Germany");
        shipment1.setCreatedAt(LocalDateTime.now().minusDays(1));

        shipment2 = new ShipmentEntity();
        shipment2.setShipmentId("SHIP-002");
        shipment2.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment2.setLastLocation("ORIGIN");
        shipment2.setDestination("Hamburg, Germany");
        shipment2.setCreatedAt(LocalDateTime.now().minusDays(2));
    }

    @Test
    @DisplayName("Should create ShipmentScannedEvent with all required immutable fields")
    void shouldCreateShipmentScannedEventWithAllRequiredImmutableFields() {
        // Given
        when(shipmentRepository.findById("SHIP-001")).thenReturn(Optional.of(shipment1));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment1);

        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);

        // When
        scanService.scanShipment("SHIP-001", "WAREHOUSE_A");

        // Then
        verify(kafkaTemplate).send(eq("shipment-scanned"), eventCaptor.capture());

        ShipmentScannedEvent capturedEvent = eventCaptor.getValue();
        assertEquals("SHIP-001", capturedEvent.getShipmentId());
        assertEquals("WAREHOUSE_A", capturedEvent.getLocation());
        assertEquals("Berlin, Germany", capturedEvent.getDestination());
        assertNotNull(capturedEvent.getScannedAt());
        assertNotNull(capturedEvent.getCorrelationId());
        assertEquals("ShipmentScannedEvent", capturedEvent.getEventType());
        assertEquals("SHIP-001", capturedEvent.getAggregateId());
    }

    @Test
    @DisplayName("Should create unique correlation IDs for different scan operations")
    void shouldCreateUniqueCorrelationIdsForDifferentScanOperations() {
        // Given – unterschiedliche Mock-Returns!
        when(shipmentRepository.findById("SHIP-001")).thenReturn(Optional.of(shipment1));
        when(shipmentRepository.findById("SHIP-002")).thenReturn(Optional.of(shipment2));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment1); // Ist ok für beide

        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);

        // When
        scanService.scanShipment("SHIP-001", "WAREHOUSE_A");
        scanService.scanShipment("SHIP-002", "WAREHOUSE_B");

        // Then
        verify(kafkaTemplate, times(2)).send(eq("shipment-scanned"), eventCaptor.capture());

        var capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());

        String correlationId1 = capturedEvents.get(0).getCorrelationId();
        String correlationId2 = capturedEvents.get(1).getCorrelationId();

        assertNotNull(correlationId1);
        assertNotNull(correlationId2);
        assertNotEquals(correlationId1, correlationId2, "Correlation IDs should be unique");

        // Prüfe auch die Shipment IDs und Locations
        assertEquals("SHIP-001", capturedEvents.get(0).getShipmentId());
        assertEquals("WAREHOUSE_A", capturedEvents.get(0).getLocation());
        assertEquals("SHIP-002", capturedEvents.get(1).getShipmentId());
        assertEquals("WAREHOUSE_B", capturedEvents.get(1).getLocation());
    }
}
