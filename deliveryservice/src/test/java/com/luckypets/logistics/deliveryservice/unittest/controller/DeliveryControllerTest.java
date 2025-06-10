package com.luckypets.logistics.deliveryservice.unittest.controller;

import com.luckypets.logistics.deliveryservice.controller.DeliveryController;
import com.luckypets.logistics.deliveryservice.exception.ShipmentNotFoundException;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.service.DeliveryService; // Keep mocking the interface
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat; // Keep argThat for verifying request objects
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Keep MockitoExtension for @Mock and @InjectMocks
@DisplayName("DeliveryController Unit Tests")
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService; // Mock the service interface

    @InjectMocks
    private DeliveryController controller; // Inject the controller

    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION_BERLIN = "Berlin";
    private static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String ERROR_MESSAGE_SHIPMENT_NOT_FOUND = "Shipment not found";


    @Test
    @DisplayName("Should return all shipments")
    void getAll_shouldReturnAllShipments() {
        // Given
        DeliveryResponse response1 = new DeliveryResponse();
        response1.setShipmentId(SHIPMENT_ID);
        response1.setStatus(STATUS_IN_TRANSIT);
        response1.setLocation("Munich");
        response1.setSuccess(true);

        DeliveryResponse response2 = new DeliveryResponse();
        response2.setShipmentId("SHIP-002");
        response2.setStatus(STATUS_DELIVERED);
        response2.setLocation("Hamburg");
        response2.setDeliveredAt(LocalDateTime.now());
        response2.setSuccess(true);

        when(deliveryService.getAllShipments()).thenReturn(Arrays.asList(response1, response2));

        // When
        List<DeliveryResponse> result = controller.getAll();

        // Then
        assertEquals(2, result.size());
        assertEquals(SHIPMENT_ID, result.get(0).getShipmentId());
        assertEquals(STATUS_IN_TRANSIT, result.get(0).getStatus());
        assertEquals("SHIP-002", result.get(1).getShipmentId());
        assertEquals(STATUS_DELIVERED, result.get(1).getStatus());
        verify(deliveryService, times(1)).getAllShipments(); // Verify service method was called
    }

    @Test
    @DisplayName("Should return shipment by ID when it exists")
    void getById_shouldReturnShipment_whenExists() {
        // Given
        DeliveryResponse response = new DeliveryResponse();
        response.setShipmentId(SHIPMENT_ID);
        response.setStatus(STATUS_IN_TRANSIT);
        response.setLocation(LOCATION_BERLIN);
        response.setSuccess(true);
        when(deliveryService.getShipmentById(SHIPMENT_ID)).thenReturn(Optional.of(response));

        // When
        ResponseEntity<DeliveryResponse> result = controller.getById(SHIPMENT_ID);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        assertEquals(STATUS_IN_TRANSIT, result.getBody().getStatus());
        verify(deliveryService, times(1)).getShipmentById(SHIPMENT_ID); // Verify service method was called
    }

    @Test
    @DisplayName("Should throw ShipmentNotFoundException when shipment does not exist")
    void getById_shouldThrowException_whenShipmentNotExists() {
        // Given
        when(deliveryService.getShipmentById("NON_EXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ShipmentNotFoundException.class, () -> controller.getById("NON_EXISTENT"));
        verify(deliveryService, times(1)).getShipmentById("NON_EXISTENT"); // Verify service method was called
    }

    @Test
    @DisplayName("Should return shipment status")
    void getStatus_shouldReturnStatus() {
        // Given
        when(deliveryService.getShipmentStatus(SHIPMENT_ID)).thenReturn(STATUS_IN_TRANSIT);

        // When
        String status = controller.getStatus(SHIPMENT_ID);

        // Then
        assertEquals(STATUS_IN_TRANSIT, status);
        verify(deliveryService, times(1)).getShipmentStatus(SHIPMENT_ID); // Verify service method was called
    }

    @Test
    @DisplayName("Should mark shipment as delivered with request body")
    void markAsDelivered_shouldMarkAsDelivered_withRequestBody() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION_BERLIN);
        DeliveryResponse successResponse = new DeliveryResponse();
        successResponse.setShipmentId(SHIPMENT_ID);
        successResponse.setStatus(STATUS_DELIVERED);
        successResponse.setDeliveredAt(LocalDateTime.now());
        successResponse.setSuccess(true);

        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(successResponse);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, request);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        assertEquals(STATUS_DELIVERED, result.getBody().getStatus());

        // Verify that the shipmentId and location in the request are set correctly before calling service
        verify(deliveryService).markAsDelivered(argThat(req ->
                SHIPMENT_ID.equals(req.getShipmentId()) && LOCATION_BERLIN.equals(req.getLocation())
        ));
    }

    @Test
    @DisplayName("Should mark shipment as delivered with null request body (location defaults to null)")
    void markAsDelivered_shouldMarkAsDelivered_withNullRequestBody() {
        // Given
        DeliveryResponse successResponse = new DeliveryResponse();
        successResponse.setShipmentId(SHIPMENT_ID);
        successResponse.setStatus(STATUS_DELIVERED);
        successResponse.setDeliveredAt(LocalDateTime.now());
        successResponse.setSuccess(true);

        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(successResponse);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, null);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        assertEquals(STATUS_DELIVERED, result.getBody().getStatus());

        // Verify that a new request is created with the shipmentId and null location
        verify(deliveryService).markAsDelivered(argThat(req ->
                SHIPMENT_ID.equals(req.getShipmentId()) && req.getLocation() == null
        ));
    }

    @Test
    @DisplayName("Should return bad request when delivery fails")
    void markAsDelivered_shouldReturnBadRequest_whenDeliveryFails() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, LOCATION_BERLIN);
        DeliveryResponse errorResponse = DeliveryResponse.error(ERROR_MESSAGE_SHIPMENT_NOT_FOUND);

        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(errorResponse);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals(ERROR_MESSAGE_SHIPMENT_NOT_FOUND, result.getBody().getErrorMessage());
        verify(deliveryService, times(1)).markAsDelivered(any(DeliveryRequest.class)); // Verify service call
    }
}
