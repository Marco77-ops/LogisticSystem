package com.luckypets.logistics.deliveryservice.unittest.controller;

import com.luckypets.logistics.deliveryservice.controller.DeliveryController;
import com.luckypets.logistics.deliveryservice.exception.ShipmentNotFoundException;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryController Unit Tests")
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryController controller;

    private static final String SHIPMENT_ID = "SHIP-001";

    @Test
    @DisplayName("Should return all shipments")
    void getAll_shouldReturnAllShipments() {
        // Given
        DeliveryResponse response1 = new DeliveryResponse(SHIPMENT_ID, "IN_TRANSIT");
        DeliveryResponse response2 = new DeliveryResponse("SHIP-002", "DELIVERED");
        when(deliveryService.getAllShipments()).thenReturn(Arrays.asList(response1, response2));

        // When
        List<DeliveryResponse> result = controller.getAll();

        // Then
        assertEquals(2, result.size());
        assertEquals(SHIPMENT_ID, result.get(0).getShipmentId());
        assertEquals("IN_TRANSIT", result.get(0).getStatus());
        assertEquals("SHIP-002", result.get(1).getShipmentId());
        assertEquals("DELIVERED", result.get(1).getStatus());
    }

    @Test
    @DisplayName("Should return shipment by ID when it exists")
    void getById_shouldReturnShipment_whenExists() {
        // Given
        DeliveryResponse response = new DeliveryResponse(SHIPMENT_ID, "IN_TRANSIT");
        when(deliveryService.getShipmentById(SHIPMENT_ID)).thenReturn(Optional.of(response));

        // When
        ResponseEntity<DeliveryResponse> result = controller.getById(SHIPMENT_ID);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        assertEquals("IN_TRANSIT", result.getBody().getStatus());
    }

    @Test
    @DisplayName("Should throw ShipmentNotFoundException when shipment does not exist")
    void getById_shouldThrowException_whenShipmentNotExists() {
        // Given
        when(deliveryService.getShipmentById("NON_EXISTENT")).thenReturn(Optional.empty());

        // Then
        assertThrows(ShipmentNotFoundException.class, () -> controller.getById("NON_EXISTENT"));
    }

    @Test
    @DisplayName("Should return shipment status")
    void getStatus_shouldReturnStatus() {
        // Given
        when(deliveryService.getShipmentStatus(SHIPMENT_ID)).thenReturn("IN_TRANSIT");

        // When
        String status = controller.getStatus(SHIPMENT_ID);

        // Then
        assertEquals("IN_TRANSIT", status);
    }

    @Test
    @DisplayName("Should mark shipment as delivered with request body")
    void markAsDelivered_shouldMarkAsDelivered_withRequestBody() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, "Berlin");
        DeliveryResponse response = new DeliveryResponse(SHIPMENT_ID, "DELIVERED");
        response.setSuccess(true);
        
        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(response);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, request);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        assertEquals("DELIVERED", result.getBody().getStatus());
        
        // Verify that the shipmentId in the request is set correctly
        verify(deliveryService).markAsDelivered(argThat(req -> 
            SHIPMENT_ID.equals(req.getShipmentId()) && "Berlin".equals(req.getLocation())
        ));
    }

    @Test
    @DisplayName("Should mark shipment as delivered with null request body")
    void markAsDelivered_shouldMarkAsDelivered_withNullRequestBody() {
        // Given
        DeliveryResponse response = new DeliveryResponse(SHIPMENT_ID, "DELIVERED");
        response.setSuccess(true);
        
        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(response);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, null);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SHIPMENT_ID, result.getBody().getShipmentId());
        
        // Verify that a new request is created with the shipmentId
        verify(deliveryService).markAsDelivered(argThat(req -> 
            SHIPMENT_ID.equals(req.getShipmentId())
        ));
    }

    @Test
    @DisplayName("Should return bad request when delivery fails")
    void markAsDelivered_shouldReturnBadRequest_whenDeliveryFails() {
        // Given
        DeliveryRequest request = new DeliveryRequest(SHIPMENT_ID, "Berlin");
        DeliveryResponse response = DeliveryResponse.error("Shipment not found");
        
        when(deliveryService.markAsDelivered(any(DeliveryRequest.class))).thenReturn(response);

        // When
        ResponseEntity<DeliveryResponse> result = controller.markAsDelivered(SHIPMENT_ID, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Shipment not found", result.getBody().getErrorMessage());
    }
}