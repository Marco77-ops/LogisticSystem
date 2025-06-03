package com.luckypets.logistics.deliveryservice.unittest.service;

import com.luckypets.logistics.deliveryservice.kafka.ShipmentDeliveredEventProducer;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryService Unit Tests")
class DeliveryServiceImplTest {

    @Mock
    private ShipmentRepository repository;

    @Mock
    private ShipmentDeliveredEventProducer eventProducer;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private ShipmentEntity testShipment;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "Berlin";
    private static final String DESTINATION = "Berlin";

    @BeforeEach
    void setUp() {
        testShipment = new ShipmentEntity();
        testShipment.setShipmentId(SHIPMENT_ID);
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        testShipment.setLastLocation("WAREHOUSE_A");
        testShipment.setOrigin("Munich");
        testShipment.setDestination(DESTINATION);
        testShipment.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should return all shipments")
    void getAllShipments_shouldReturnAllShipments() {
        // Given
        ShipmentEntity shipment1 = new ShipmentEntity();
        shipment1.setShipmentId("SHIP-001");
        shipment1.setStatus(ShipmentStatus.IN_TRANSIT);

        ShipmentEntity shipment2 = new ShipmentEntity();
        shipment2.setShipmentId("SHIP-002");
        shipment2.setStatus(ShipmentStatus.DELIVERED);

        when(repository.findAll()).thenReturn(Arrays.asList(shipment1, shipment2));

        // When
        List<DeliveryResponse> result = deliveryService.getAllShipments();

        // Then
        assertEquals(2, result.size());
        assertEquals("SHIP-001", result.get(0).getShipmentId());
        assertEquals("IN_TRANSIT", result.get(0).getStatus());
        assertEquals("SHIP-002", result.get(1).getShipmentId());
        assertEquals("DELIVERED", result.get(1).getStatus());
    }

    @Test
    @DisplayName("Should return shipment by ID when it exists")
    void getShipmentById_shouldReturnShipment_whenExists() {
        // Given
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));

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
        // Given
        when(repository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

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
        // Given
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));

        // When
        String status = deliveryService.getShipmentStatus(SHIPMENT_ID);

        // Then
        assertEquals("IN_TRANSIT", status);
    }

    @Test
    @DisplayName("Should return 'Unknown' when shipment does not exist")
    void getShipmentStatus_shouldReturnUnknown_whenShipmentNotExists() {
        // Given
        when(repository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

        // When
        String status = deliveryService.getShipmentStatus("NON_EXISTENT");

        // Then
        assertEquals("Unknown", status);
    }

    @Test
    @DisplayName("Should mark shipment as delivered and send event")
    void markAsDelivered_shouldUpdateShipmentAndSendEvent() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION);
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));
        when(repository.save(any(ShipmentEntity.class))).thenReturn(testShipment);

        // When
        DeliveryResponse response = deliveryService.markAsDelivered(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(SHIPMENT_ID, response.getShipmentId());
        
        // Verify shipment was updated
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(shipmentCaptor.capture());
        ShipmentEntity savedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, savedShipment.getStatus());
        assertEquals(LOCATION, savedShipment.getLastLocation());
        assertNotNull(savedShipment.getDeliveredAt());
        
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
    @DisplayName("Should return error when shipment not found")
    void markAsDelivered_shouldReturnError_whenShipmentNotFound() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION);
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.empty());

        // When
        DeliveryResponse response = deliveryService.markAsDelivered(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Shipment not found: " + SHIPMENT_ID, response.getErrorMessage());
        verify(repository, never()).save(any());
        verify(eventProducer, never()).sendShipmentDeliveredEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void markAsDelivered_shouldThrowException_whenRequestIsNull() {
        // Then
        assertThrows(IllegalArgumentException.class, () -> deliveryService.markAsDelivered(null));
    }

    @Test
    @DisplayName("Should throw exception when shipment ID in request is null or empty")
    void markAsDelivered_shouldThrowException_whenShipmentIdIsNullOrEmpty() {
        // Given
        DeliveryRequest requestWithNullId = new DeliveryRequest(null, LOCATION);
        DeliveryRequest requestWithEmptyId = new DeliveryRequest("", LOCATION);

        // Then
        assertThrows(IllegalArgumentException.class, () -> deliveryService.markAsDelivered(requestWithNullId));
        assertThrows(IllegalArgumentException.class, () -> deliveryService.markAsDelivered(requestWithEmptyId));
    }

    @Test
    @DisplayName("Should throw exception when location in request is null or empty")
    void markAsDelivered_shouldThrowException_whenLocationIsNullOrEmpty() {
        // Given
        DeliveryRequest requestWithNullLocation = new DeliveryRequest(SHIPMENT_ID, null);
        DeliveryRequest requestWithEmptyLocation = new DeliveryRequest(SHIPMENT_ID, "");

        // Then
        assertThrows(IllegalArgumentException.class, () -> deliveryService.markAsDelivered(requestWithNullLocation));
        assertThrows(IllegalArgumentException.class, () -> deliveryService.markAsDelivered(requestWithEmptyLocation));
    }

    @Test
    @DisplayName("Should find shipment entity by ID")
    void findShipmentEntityById_shouldReturnShipment_whenExists() {
        // Given
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(testShipment));

        // When
        Optional<ShipmentEntity> result = deliveryService.findShipmentEntityById(SHIPMENT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testShipment, result.get());
    }

    @Test
    @DisplayName("Should return empty when shipment entity not found")
    void findShipmentEntityById_shouldReturnEmpty_whenNotExists() {
        // Given
        when(repository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

        // When
        Optional<ShipmentEntity> result = deliveryService.findShipmentEntityById("NON_EXISTENT");

        // Then
        assertFalse(result.isPresent());
    }
}