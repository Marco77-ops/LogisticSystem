package com.luckypets.logistics.shipmentservice.unittest.service;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shipmentservice.service.ShipmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentServiceImplTest {

    private ShipmentRepository repository;
    private ShipmentEventProducer eventProducer;
    private ShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ShipmentRepository.class);
        eventProducer = mock(ShipmentEventProducer.class);
        service = new ShipmentServiceImpl(repository, eventProducer);
    }

    @Test
    void createShipment_shouldSaveAndPublishEvent() {
        // Arrange
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Munich");
        request.setDestination("Berlin");
        request.setCustomerId("C123");

        ShipmentEntity saved = new ShipmentEntity();
        saved.setShipmentId("test-id");
        saved.setOrigin("Munich");
        saved.setDestination("Berlin");
        saved.setCustomerId("C123");
        saved.setStatus(ShipmentStatus.CREATED);

        when(repository.save(any(ShipmentEntity.class))).thenReturn(saved);

        // Act
        ShipmentEntity result = service.createShipment(request);

        // Assert
        assertEquals(saved, result);
        verify(repository, times(1)).save(any(ShipmentEntity.class));
        verify(eventProducer, times(1)).sendShipmentCreatedEvent(any(ShipmentCreatedEvent.class));
    }

    @Test
    void getShipmentById_found_returnsOptionalWithEntity() {
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId("id1");
        when(repository.findById("id1")).thenReturn(Optional.of(entity));

        Optional<ShipmentEntity> found = service.getShipmentById("id1");
        assertTrue(found.isPresent());
        assertEquals(entity, found.get());
    }

    @Test
    void getShipmentById_notFound_returnsEmpty() {
        when(repository.findById("notThere")).thenReturn(Optional.empty());
        Optional<ShipmentEntity> found = service.getShipmentById("notThere");
        assertFalse(found.isPresent());
    }

    @Test
    void getAllShipments_returnsList() {
        ShipmentEntity e1 = new ShipmentEntity();
        e1.setShipmentId("a");
        ShipmentEntity e2 = new ShipmentEntity();
        e2.setShipmentId("b");
        List<ShipmentEntity> list = Arrays.asList(e1, e2);
        when(repository.findAll()).thenReturn(list);

        List<ShipmentEntity> result = service.getAllShipments();
        assertEquals(list, result);
    }

    @Test
    void deleteShipment_exists_deletesAndReturnsTrue() {
        when(repository.existsById("del")).thenReturn(true);
        doNothing().when(repository).deleteById("del");

        boolean result = service.deleteShipment("del");
        assertTrue(result);
        verify(repository, times(1)).deleteById("del");
    }

    @Test
    void deleteShipment_notExists_returnsFalse() {
        when(repository.existsById("missing")).thenReturn(false);

        boolean result = service.deleteShipment("missing");
        assertFalse(result);
        verify(repository, never()).deleteById(anyString());
    }
}
