package com.luckypets.logistics.deliveryservice.unittest.service;

import com.luckypets.logistics.deliveryservice.kafka.ShipmentDeliveredEventProducer;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.model.ShipmentEntity; // This will be the local POJO
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl; // Import the concrete implementation
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito; // Import Mockito class for static mock method

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DeliveryServiceImpl Unit Tests")
class DeliveryServiceImplTest {

    @Mock
    private ShipmentDeliveredEventProducer eventProducer;

    private DeliveryServiceImpl deliveryService;

    private ShipmentEntity testShipment;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "Berlin";
    private static final String DESTINATION = "Berlin";

    @BeforeEach
    void setUp() {
        eventProducer = Mockito.mock(ShipmentDeliveredEventProducer.class);
        deliveryService = new DeliveryServiceImpl(eventProducer);
        deliveryService.clearInMemoryStorageForTests();

        testShipment = new ShipmentEntity(
                SHIPMENT_ID, "OriginCity", DESTINATION, "CUST001", LocalDateTime.now().minusDays(1), ShipmentStatus.IN_TRANSIT
        );
        testShipment.setLastLocation("WAREHOUSE_A");
        deliveryService.addShipmentForTest(testShipment);
    }

    @Test
    @DisplayName("Should return all shipments")
    void getAllShipments_shouldReturnAllShipments() {
        // Given
        ShipmentEntity shipment1 = new ShipmentEntity("SHIP-001", "O1", "D1", "C1", LocalDateTime.now(), ShipmentStatus.IN_TRANSIT);
        ShipmentEntity shipment2 = new ShipmentEntity("SHIP-002", "O2", "D2", "C2", LocalDateTime.now(), ShipmentStatus.DELIVERED);

        deliveryService.addShipmentForTest(shipment1);
        deliveryService.addShipmentForTest(shipment2);

        // When
        List<DeliveryResponse> result = deliveryService.getAllShipments();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(res -> res.getShipmentId().equals("SHIP-001") && res.getStatus().equals("IN_TRANSIT")));
        assertTrue(result.stream().anyMatch(res -> res.getShipmentId().equals("SHIP-002") && res.getStatus().equals("DELIVERED")));
    }

    @Test
    @DisplayName("Should return shipment by ID when it exists")
    void getShipmentById_shouldReturnShipment_whenExists() {
        // Given (testShipment already added in @BeforeEach)

        // When
        Optional<DeliveryResponse> result = deliveryService.getShipmentById(SHIPMENT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(SHIPMENT_ID, result.get().getShipmentId());
        assertEquals("IN_TRANSIT", result.get().getStatus());
    }

    @Test
    @DisplayName("Should return empty when shipment ID does not exist")
    void getShipmentById_shouldReturnEmpty_whenNotExists() {
        // Given (no additional setup needed, testShipment is the only one in memory)

        // When
        Optional<DeliveryResponse> result = deliveryService.getShipmentById("NON_EXISTENT");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should throw exception when shipment ID is null or empty")
    void getShipmentById_shouldThrowException_whenShipmentIdIsNullOrEmpty() {
        // Then
        assertThrows(IllegalArgumentException.class, () -> deliveryService.getShipmentById(null));
        assertThrows(IllegalArgumentException.class, () -> deliveryService.getShipmentById(""));
    }

    @Test
    @DisplayName("Should return status when shipment exists")
    void getShipmentStatus_shouldReturnStatus_whenShipmentExists() {
        // Given (testShipment already added in @BeforeEach)

        // When
        String status = deliveryService.getShipmentStatus(SHIPMENT_ID);

        // Then
        assertEquals("IN_TRANSIT", status);
    }

    @Test
    @DisplayName("Should return 'Unknown' when shipment does not exist")
    void getShipmentStatus_shouldReturnUnknown_whenShipmentNotExists() {
        // Given (no additional setup needed)

        // When
        String status = deliveryService.getShipmentStatus("NON_EXISTENT");

        // Then
        assertEquals("Unknown", status);
    }

    @Test
    @DisplayName("Should mark shipment as delivered and send event")
    void markAsDelivered_shouldUpdateShipmentAndSendEvent() {
        // Given (testShipment already added in @BeforeEach)
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION);

        // When
        DeliveryResponse response = deliveryService.markAsDelivered(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(SHIPMENT_ID, response.getShipmentId());

        // Verify shipment was updated in in-memory storage
        Optional<ShipmentEntity> updatedShipmentOpt = deliveryService.findShipmentEntityById(SHIPMENT_ID);
        assertTrue(updatedShipmentOpt.isPresent());
        ShipmentEntity updatedShipment = updatedShipmentOpt.get();
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getDeliveredAt());

        // Verify event was sent
        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(eventProducer).sendShipmentDeliveredEvent(eventCaptor.capture());
        ShipmentDeliveredEvent event = eventCaptor.getValue();
        assertEquals(SHIPMENT_ID, event.getShipmentId());
        assertEquals(DESTINATION, event.getDestination());
        assertEquals(LOCATION, event.getLocation());
        assertNotNull(event.getCorrelationId());
    }

    @Test
    @DisplayName("Should return error when shipment not found for delivery")
    void markAsDelivered_shouldReturnError_whenShipmentNotFound() {
        // Given: Clear storage to ensure shipment is not found
        deliveryService.clearInMemoryStorageForTests();
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION);

        // When
        DeliveryResponse response = deliveryService.markAsDelivered(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Shipment not found: " + SHIPMENT_ID, response.getErrorMessage());
        verify(eventProducer, never()).sendShipmentDeliveredEvent(any()); // No event should be sent
    }

    @Test
    @DisplayName("Should return error when request is null")
    void markAsDelivered_shouldReturnError_whenRequestIsNull() {
        // When
        DeliveryResponse response = deliveryService.markAsDelivered(null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Request must not be null", response.getErrorMessage());
        verify(eventProducer, never()).sendShipmentDeliveredEvent(any());
    }

    @Test
    @DisplayName("Should return error when shipment ID in request is null or empty")
    void markAsDelivered_shouldReturnError_whenShipmentIdIsNullOrEmpty() {
        // Given
        DeliveryRequest requestWithNullId = new DeliveryRequest(null, LOCATION);
        DeliveryRequest requestWithEmptyId = new DeliveryRequest("", LOCATION);

        // When
        DeliveryResponse responseNullId = deliveryService.markAsDelivered(requestWithNullId);
        DeliveryResponse responseEmptyId = deliveryService.markAsDelivered(requestWithEmptyId);

        // Then
        assertFalse(responseNullId.isSuccess());
        assertEquals("shipmentId must not be null or empty", responseNullId.getErrorMessage());
        assertFalse(responseEmptyId.isSuccess());
        assertEquals("shipmentId must not be null or empty", responseEmptyId.getErrorMessage());
        verify(eventProducer, never()).sendShipmentDeliveredEvent(any());
    }

    @Test
    @DisplayName("Should return error when location in request is null or empty")
    void markAsDelivered_shouldReturnError_whenLocationIsNullOrEmpty() {
        // Given
        DeliveryRequest requestWithNullLocation = new DeliveryRequest(SHIPMENT_ID, null);
        DeliveryRequest requestWithEmptyLocation = new DeliveryRequest(SHIPMENT_ID, "");

        // When
        DeliveryResponse responseNullLocation = deliveryService.markAsDelivered(requestWithNullLocation);
        DeliveryResponse responseEmptyLocation = deliveryService.markAsDelivered(requestWithEmptyLocation);

        // Then
        assertFalse(responseNullLocation.isSuccess());
        assertEquals("location must not be null or empty", responseNullLocation.getErrorMessage());
        assertFalse(responseEmptyLocation.isSuccess());
        assertEquals("location must not be null or empty", responseEmptyLocation.getErrorMessage());
        verify(eventProducer, never()).sendShipmentDeliveredEvent(any());
    }

    @Test
    @DisplayName("Should find shipment entity by ID")
    void findShipmentEntityById_shouldReturnShipment_whenExists() {
        // Given (testShipment already added in @BeforeEach)

        // When
        Optional<ShipmentEntity> result = deliveryService.findShipmentEntityById(SHIPMENT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testShipment, result.get());
    }

    @Test
    @DisplayName("Should return empty when shipment entity not found")
    void findShipmentEntityById_shouldReturnEmpty_whenNotExists() {
        // Given: Clear storage to ensure shipment is not found
        deliveryService.clearInMemoryStorageForTests();

        // When
        Optional<ShipmentEntity> result = deliveryService.findShipmentEntityById("NON_EXISTENT");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update shipment state via updateShipmentState method")
    void updateShipmentState_shouldUpdateShipmentInStorage() {
        // Given: A new shipment entity
        ShipmentEntity newShipment = new ShipmentEntity("NEW-SHIP", "Start", "End", "CUST002", LocalDateTime.now(), ShipmentStatus.CREATED);

        // When
        deliveryService.updateShipmentState(newShipment);

        // Then
        Optional<ShipmentEntity> retrieved = deliveryService.findShipmentEntityById("NEW-SHIP");
        assertTrue(retrieved.isPresent());
        assertEquals(newShipment, retrieved.get());

        // Update an existing one
        newShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        newShipment.setLastLocation("Midway");
        deliveryService.updateShipmentState(newShipment);

        retrieved = deliveryService.findShipmentEntityById("NEW-SHIP");
        assertTrue(retrieved.isPresent());
        assertEquals(ShipmentStatus.IN_TRANSIT, retrieved.get().getStatus());
        assertEquals("Midway", retrieved.get().getLastLocation());
    }
}
