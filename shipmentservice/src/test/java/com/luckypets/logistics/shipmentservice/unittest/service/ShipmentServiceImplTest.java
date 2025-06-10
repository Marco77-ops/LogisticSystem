package com.luckypets.logistics.shipmentservice.unittest.service;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
// Import UUID for mocking

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentServiceImplTest {

    private ShipmentEventProducer eventProducer;
    private ShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        eventProducer = mock(ShipmentEventProducer.class);
        // Correctly initialize the service with the mocked producer
        service = new ShipmentServiceImpl(eventProducer);
    }

    @Test
    void createShipment_shouldSaveAndPublishEvent() {
        // Arrange
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Munich");
        request.setDestination("Berlin");
        request.setCustomerId("C123");

        // Act
        ShipmentEntity result = service.createShipment(request); // This line caused NullPointerException

        // Assert
        assertNotNull(result);
        assertNotNull(result.getShipmentId());
        assertEquals("Munich", result.getOrigin());
        assertEquals("Berlin", result.getDestination());
        assertEquals("C123", result.getCustomerId());
        assertEquals(ShipmentStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());

        // Verify that ShipmentCreatedEvent was sent
        verify(eventProducer, times(1)).sendShipmentCreatedEvent(any(ShipmentCreatedEvent.class));

        // Additionally, verify that the created shipment can be retrieved from in-memory storage
        Optional<ShipmentEntity> retrieved = service.getShipmentById(result.getShipmentId());
        assertTrue(retrieved.isPresent());
        assertEquals(result.getShipmentId(), retrieved.get().getShipmentId());
    }

    @Test
    void getShipmentById_found_returnsOptionalWithEntity() {
        // Arrange: Create a shipment through the service so it's in the in-memory map
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("OriginTest");
        request.setDestination("DestinationTest");
        request.setCustomerId("CustomerTest");
        ShipmentEntity createdEntity = service.createShipment(request);
        String shipmentId = createdEntity.getShipmentId();

        // Act
        Optional<ShipmentEntity> found = service.getShipmentById(shipmentId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(createdEntity, found.get());
    }

    @Test
    void getShipmentById_notFound_returnsEmpty() {
        // Act
        Optional<ShipmentEntity> found = service.getShipmentById("notThere");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void getAllShipments_returnsList() {
        // Arrange: Create some shipments through the service
        service.createShipment(new ShipmentRequest() {{ setOrigin("O1"); setDestination("D1"); setCustomerId("C1"); }});
        service.createShipment(new ShipmentRequest() {{ setOrigin("O2"); setDestination("D2"); setCustomerId("C2"); }});

        // Act
        List<ShipmentEntity> result = service.getAllShipments();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Should contain the 2 created shipments
    }

    @Test
    void deleteShipment_exists_deletesAndReturnsTrue() {
        // Arrange: Create a shipment to delete
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("OriginDel");
        request.setDestination("DestinationDel");
        request.setCustomerId("CustDel");
        ShipmentEntity created = service.createShipment(request);
        String shipmentIdToDelete = created.getShipmentId();

        // Act
        boolean result = service.deleteShipment(shipmentIdToDelete);

        // Assert
        assertTrue(result);
        assertFalse(service.getShipmentById(shipmentIdToDelete).isPresent()); // Verify it's deleted
    }

    @Test
    void deleteShipment_notExists_returnsFalse() {
        // Act
        boolean result = service.deleteShipment("nonExistentId");

        // Assert
        assertFalse(result);
    }
}
