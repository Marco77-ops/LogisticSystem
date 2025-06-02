package com.luckypets.logistics.shipmentservice.unittest.controller;

import com.luckypets.logistics.shipmentservice.controller.ShipmentController;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShipmentControllerTest {

    private ShipmentService shipmentService;
    private ShipmentController shipmentController;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        shipmentController = new ShipmentController(shipmentService);
    }

    @Test
    void createShipment_shouldReturnCreated() {
        // Arrange
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Origin");
        request.setDestination("Destination");
        request.setCustomerId("CUST001");

        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId("S1");
        entity.setOrigin("Origin");
        entity.setDestination("Destination");
        entity.setCustomerId("CUST001");
        entity.setCreatedAt(LocalDateTime.now());

        when(shipmentService.createShipment(any(ShipmentRequest.class))).thenReturn(entity);

        // Act
        ResponseEntity<ShipmentEntity> response = shipmentController.createShipment(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(entity, response.getBody());
    }

    @Test
    void getShipmentById_existing_returnsOk() {
        // Arrange
        String id = "S1";
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId(id);

        when(shipmentService.getShipmentById(id)).thenReturn(Optional.of(entity));

        // Act
        ResponseEntity<ShipmentEntity> response = shipmentController.getShipmentById(id);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(entity, response.getBody());
    }

    @Test
    void getShipmentById_notFound_returns404() {
        // Arrange
        String id = "S999";
        when(shipmentService.getShipmentById(id)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<ShipmentEntity> response = shipmentController.getShipmentById(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getAllShipments_returnsList() {
        // Arrange
        ShipmentEntity e1 = new ShipmentEntity();
        e1.setShipmentId("S1");
        ShipmentEntity e2 = new ShipmentEntity();
        e2.setShipmentId("S2");
        List<ShipmentEntity> list = Arrays.asList(e1, e2);

        when(shipmentService.getAllShipments()).thenReturn(list);

        // Act
        ResponseEntity<List<ShipmentEntity>> response = shipmentController.getAllShipments();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(list, response.getBody());
    }

    @Test
    void deleteShipment_existing_returnsNoContent() {
        // Arrange
        String id = "S1";
        when(shipmentService.deleteShipment(id)).thenReturn(true);

        // Act
        ResponseEntity<Void> response = shipmentController.deleteShipment(id);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteShipment_notExisting_returnsNotFound() {
        // Arrange
        String id = "S999";
        when(shipmentService.deleteShipment(id)).thenReturn(false);

        // Act
        ResponseEntity<Void> response = shipmentController.deleteShipment(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
