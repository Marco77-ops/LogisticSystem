package com.luckypets.logistics.scanservice.unittest;

import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.model.ShipmentEntity;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ScanService Unit Tests")
class ScanServiceTest {

    private KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;
    private ScanServiceImpl scanService;
    private ShipmentEntity testShipment;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin, Germany";

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        scanService = new ScanServiceImpl(kafkaTemplate);
        scanService.clearInMemoryStorageForTests();

        testShipment = new ShipmentEntity();
        testShipment.setShipmentId(SHIPMENT_ID);
        testShipment.setOrigin("ORIGIN_CITY");
        testShipment.setDestination(DESTINATION);
        testShipment.setCustomerId("CUSTOMER_123");
        testShipment.setStatus(ShipmentStatus.CREATED);
        testShipment.setCreatedAt(LocalDateTime.now().minusDays(1));
        testShipment.setLastLocation("INITIAL_LOCATION");
        testShipment.setLastScannedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should successfully scan existing shipment")
    void shouldScanExistingShipment() {
        scanService.addShipmentForTest(testShipment);
        ScanResult result = scanService.scanShipment(SHIPMENT_ID, LOCATION);

        assertTrue(result.isSuccess());
        assertEquals(SHIPMENT_ID, result.getShipmentId());

        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        ShipmentEntity updatedShipment = updatedShipmentOpt.get();
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getLastScannedAt());
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus());

        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                eventCaptor.capture()
        );
        ShipmentScannedEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(SHIPMENT_ID, capturedEvent.getShipmentId());
        assertEquals(LOCATION, capturedEvent.getLocation());
        assertEquals(DESTINATION, capturedEvent.getDestination());
        assertNotNull(capturedEvent.getScannedAt());
        assertNotNull(capturedEvent.getCorrelationId());
        assertThat(capturedEvent.getCorrelationId()).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail when shipment not found")
    void shouldFailWhenShipmentNotFound() {
        ScanResult result = scanService.scanShipment("NON_EXISTENT_ID", LOCATION);
        assertFalse(result.isSuccess());
        assertEquals("Shipment with ID NON_EXISTENT_ID not found.", result.getErrorMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle null shipment ID")
    void shouldHandleNullShipmentId() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(null, LOCATION));
        assertEquals("Shipment ID cannot be null", thrown.getMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle null location")
    void shouldHandleNullLocation() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(SHIPMENT_ID, null));
        assertEquals("Location cannot be null", thrown.getMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty location")
    void shouldHandleEmptyLocation() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(SHIPMENT_ID, ""));
        assertEquals("Location cannot be empty", thrown.getMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should update shipment location and status after scan")
    void shouldUpdateShipmentLocationAfterScan() {
        scanService.addShipmentForTest(testShipment);
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        ShipmentEntity updatedShipment = updatedShipmentOpt.get();
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getLastScannedAt());
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus());
    }

    @Test
    @DisplayName("Should create correct ShipmentScannedEvent with immutable constructor")
    void shouldCreateCorrectShipmentScannedEvent() {
        scanService.addShipmentForTest(testShipment);
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                eventCaptor.capture()
        );
        ShipmentScannedEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(SHIPMENT_ID, capturedEvent.getShipmentId());
        assertEquals(LOCATION, capturedEvent.getLocation());
        assertEquals(DESTINATION, capturedEvent.getDestination());
        assertNotNull(capturedEvent.getScannedAt());
        assertNotNull(capturedEvent.getCorrelationId());
        assertThat(capturedEvent.getCorrelationId()).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate correlation ID for ShipmentScannedEvent")
    void shouldGenerateCorrelationIdForShipmentScannedEvent() {
        scanService.addShipmentForTest(testShipment);
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                eventCaptor.capture()
        );
        ShipmentScannedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getCorrelationId()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should not change status if already DELIVERED")
    void shouldNotChangeStatusIfAlreadyDelivered() {
        testShipment.setStatus(ShipmentStatus.DELIVERED);
        scanService.addShipmentForTest(testShipment);
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        assertEquals(ShipmentStatus.DELIVERED, updatedShipmentOpt.get().getStatus());
        verify(kafkaTemplate).send(anyString(), anyString(), any(ShipmentScannedEvent.class));
    }
}
