package com.luckypets.logistics.shipmentservice.service;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    // In-memory storage
    private final ConcurrentHashMap<String, ShipmentEntity> inMemoryStorage = new ConcurrentHashMap<>();
    private final ShipmentEventProducer eventProducer;

    public ShipmentServiceImpl(ShipmentEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @Override
    public ShipmentEntity createShipment(ShipmentRequest request) {
        ShipmentEntity entity = new ShipmentEntity();
        String shipmentId = UUID.randomUUID().toString().substring(0, 8);
        entity.setShipmentId(shipmentId);
        entity.setOrigin(request.getOrigin());
        entity.setDestination(request.getDestination());
        entity.setCustomerId(request.getCustomerId());
        entity.setStatus(ShipmentStatus.CREATED);
        entity.setCreatedAt(LocalDateTime.now());

        // Store in memory
        inMemoryStorage.put(shipmentId, entity);

        // Create and publish ShipmentCreatedEvent
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                entity.getShipmentId(),
                entity.getDestination(),
                entity.getCreatedAt(),
                UUID.randomUUID().toString()
        );
        eventProducer.sendShipmentCreatedEvent(event);

        return entity;
    }

    @Override
    public Optional<ShipmentEntity> getShipmentById(String shipmentId) {
        // Retrieve from in-memory storage
        return Optional.ofNullable(inMemoryStorage.get(shipmentId));
    }

    @Override
    public List<ShipmentEntity> getAllShipments() {
        // Retrieve all from in-memory storage
        return new ArrayList<>(inMemoryStorage.values());
    }

    @Override
    public boolean deleteShipment(String shipmentId) {
        // Remove from in-memory storage
        return inMemoryStorage.remove(shipmentId) != null;
    }

    /**
     * Helper method for clearing the in-memory storage, used by integration tests.
     * This ensures a clean state for each test run.
     */
    public void clearInMemoryStorageForTests() {
        inMemoryStorage.clear();
    }
}
