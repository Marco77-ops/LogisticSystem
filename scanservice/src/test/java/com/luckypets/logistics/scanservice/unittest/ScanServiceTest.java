package com.luckypets.logistics.scanservice.unittest;

import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.persistence.ShipmentRepository;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanService Unit Tests")
class ScanServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    @InjectMocks
    private ScanServiceImpl scanService;

    private ShipmentEntity testShipment;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin, Germany";

    @BeforeEach
    void setUp() {
        testShipment = new ShipmentEntity();
        testShipment.setShipmentId(SHIPMENT_ID);
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        testShipment.setLastLocation("ORIGIN");
        testShipment.setDestination(DESTINATION);
        testShipment.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should successfully scan existing shipment")
    void shouldScanExistingShipment() {
        // Given
        when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(testShipment);

        // When
        ScanResult result = scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SHIPMENT_ID, result.getShipmentId());
        verify(shipmentRepository).findById(SHIPMENT_ID);
        verify(shipmentRepository).save(any(ShipmentEntity.class));
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                any(ShipmentScannedEvent.class)
        );
    }

    @Test
    @DisplayName("Should fail when shipment not found")
    void shouldFailWhenShipmentNotFound() {
        // Given
        when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.empty());

        // When
        ScanResult result = scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Shipment not found", result.getErrorMessage());
        verify(shipmentRepository).findById(SHIPMENT_ID);
        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle null shipment ID")
    void shouldHandleNullShipmentId() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(null, LOCATION));

        verify(shipmentRepository, never()).findById(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty location")
    void shouldHandleEmptyLocation() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(SHIPMENT_ID, ""));

        verify(shipmentRepository, never()).findById(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should update shipment location after scan")
    void shouldUpdateShipmentLocationAfterScan() {
        // Given
        when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        verify(shipmentRepository).save(argThat(shipment ->
                LOCATION.equals(shipment.getLastLocation()) &&
                        shipment.getLastScannedAt() != null
        ));
    }

    @Test
    @DisplayName("Should create correct ShipmentScannedEvent with immutable constructor")
    void shouldCreateCorrectShipmentScannedEvent() {
        // Given
        when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                argThat(event ->
                        SHIPMENT_ID.equals(event.getShipmentId()) &&
                                LOCATION.equals(event.getLocation()) &&
                                DESTINATION.equals(event.getDestination()) &&
                                event.getScannedAt() != null &&
                                event.getCorrelationId() != null
                )
        );
    }

    @Test
    @DisplayName("Should generate correlation ID for ShipmentScannedEvent")
    void shouldGenerateCorrelationIdForShipmentScannedEvent() {
        // Given
        when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                argThat(event ->
                        event.getCorrelationId() != null &&
                                !event.getCorrelationId().isEmpty()
                )
        );
    }
}
