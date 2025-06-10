package com.luckypets.logistics.scanservice.unittest;

import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.scanservice.model.ShipmentEntity; // Import local ShipmentEntity
import com.luckypets.logistics.shared.model.ShipmentStatus; // Import shared ShipmentStatus
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Removed @ExtendWith(MockitoExtension.class) and @InjectMocks for explicit setup
// since ScanServiceImpl now only takes KafkaTemplate and we're managing its internal state directly.
@DisplayName("ScanService Unit Tests")
class ScanServiceTest {

    @Mock
    private KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    // Use concrete type and instantiate manually, as we're managing its in-memory state
    private ScanServiceImpl scanService;

    // testShipment is now a plain object that we'll add to the service's in-memory map
    private ShipmentEntity testShipment;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin, Germany";

    @BeforeEach
    void setUp() {
        // Initialize the mock KafkaTemplate
        kafkaTemplate = mock(KafkaTemplate.class);
        // Manually instantiate ScanServiceImpl with the mocked KafkaTemplate
        scanService = new ScanServiceImpl(kafkaTemplate);
        // Clear the in-memory storage for clean test isolation
        scanService.clearInMemoryStorageForTests();

        // Initialize testShipment POJO
        testShipment = new ShipmentEntity();
        testShipment.setShipmentId(SHIPMENT_ID);
        testShipment.setOrigin("ORIGIN_CITY"); // Added origin for completeness
        testShipment.setDestination(DESTINATION);
        testShipment.setCustomerId("CUSTOMER_123"); // Added customerId
        testShipment.setStatus(ShipmentStatus.CREATED); // Initial status
        testShipment.setCreatedAt(LocalDateTime.now().minusDays(1));
        testShipment.setLastLocation("INITIAL_LOCATION"); // Initial location before scan
        testShipment.setLastScannedAt(LocalDateTime.now().minusDays(1)); // Initial scanned time
    }

    @Test
    @DisplayName("Should successfully scan existing shipment")
    void shouldScanExistingShipment() {
        // Arrange: Add the testShipment to the service's in-memory storage
        scanService.addShipmentForTest(testShipment);

        // When
        ScanResult result = scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SHIPMENT_ID, result.getShipmentId());

        // Verify the in-memory state was updated
        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        ShipmentEntity updatedShipment = updatedShipmentOpt.get();
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getLastScannedAt());
        // Verify status was updated to IN_TRANSIT
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus());


        // Verify Kafka event was sent
        ArgumentCaptor<ShipmentScannedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentScannedEvent.class);
        verify(kafkaTemplate).send(
                eq("shipment-scanned"),
                eq(SHIPMENT_ID),
                eventCaptor.capture() // Capture the sent event
        );

        // Assert on the captured event's properties
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
        // Arrange: Do NOT add the testShipment to the service's in-memory storage

        // When
        ScanResult result = scanService.scanShipment("NON_EXISTENT_ID", LOCATION); // Use a non-existent ID

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Shipment not found", result.getErrorMessage());
        // Verify no Kafka event was sent
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle null shipment ID")
    void shouldHandleNullShipmentId() {
        // Arrange (no specific arrangement needed for this negative test)

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(null, LOCATION));

        assertEquals("Shipment ID cannot be null", thrown.getMessage());
        // Verify no Kafka event was sent
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty location")
    void shouldHandleEmptyLocation() {
        // Arrange (no specific arrangement needed for this negative test)

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> scanService.scanShipment(SHIPMENT_ID, ""));

        assertEquals("Location cannot be empty", thrown.getMessage());
        // Verify no Kafka event was sent
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should update shipment location and status after scan")
    void shouldUpdateShipmentLocationAfterScan() {
        // Arrange: Add the testShipment to the service's in-memory storage
        scanService.addShipmentForTest(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then: Verify the in-memory state was updated
        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        ShipmentEntity updatedShipment = updatedShipmentOpt.get();

        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getLastScannedAt());
        // Check that the status changed to IN_TRANSIT
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus());
    }

    @Test
    @DisplayName("Should create correct ShipmentScannedEvent with immutable constructor")
    void shouldCreateCorrectShipmentScannedEvent() {
        // Arrange: Add the testShipment to the service's in-memory storage
        scanService.addShipmentForTest(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
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
        // Arrange: Add the testShipment to the service's in-memory storage
        scanService.addShipmentForTest(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
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
        // Arrange: Set status to DELIVERED
        testShipment.setStatus(ShipmentStatus.DELIVERED);
        scanService.addShipmentForTest(testShipment);

        // When
        scanService.scanShipment(SHIPMENT_ID, LOCATION);

        // Then
        Optional<ShipmentEntity> updatedShipmentOpt = scanService.findById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        assertEquals(ShipmentStatus.DELIVERED, updatedShipmentOpt.get().getStatus()); // Status should remain DELIVERED
        // Still verify event sent as scan still happens
        verify(kafkaTemplate).send(anyString(), anyString(), any(ShipmentScannedEvent.class));
    }
}
