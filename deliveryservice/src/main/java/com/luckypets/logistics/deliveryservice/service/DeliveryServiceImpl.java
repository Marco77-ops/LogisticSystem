package com.luckypets.logistics.deliveryservice.service;

import com.luckypets.logistics.deliveryservice.kafka.ShipmentDeliveredEventProducer;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.model.ShipmentEntity; // This will be the local POJO
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Keep @Transactional for method-level atomicity if desired, even without DB

import java.time.LocalDateTime;
import java.util.ArrayList; // Used for getAllShipments
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // For in-memory storage
import java.util.stream.Collectors;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);
    // Replace ShipmentRepository with in-memory storage
    private final ConcurrentHashMap<String, ShipmentEntity> inMemoryStorage = new ConcurrentHashMap<>();
    private final ShipmentDeliveredEventProducer eventProducer;

    // Update constructor to no longer take ShipmentRepository
    public DeliveryServiceImpl(ShipmentDeliveredEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @Override
    public List<DeliveryResponse> getAllShipments() {
        // Retrieve all from in-memory storage
        return new ArrayList<>(inMemoryStorage.values()).stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeliveryResponse> getShipmentById(String shipmentId) {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be null or empty");
        }
        // Retrieve from in-memory storage
        return Optional.ofNullable(inMemoryStorage.get(shipmentId))
                .map(this::mapToDeliveryResponse);
    }

    @Override
    public String getShipmentStatus(String shipmentId) {
        // Retrieve from in-memory storage
        return Optional.ofNullable(inMemoryStorage.get(shipmentId))
                .map(shipment -> shipment.getStatus().name())
                .orElse("Unknown");
    }

    @Override
    @Transactional // Keep for logical transaction, even if no DB transaction
    public DeliveryResponse markAsDelivered(DeliveryRequest request) {
        if (request == null) {
            return DeliveryResponse.error("Request must not be null"); // Changed to return error
        }
        if (request.getShipmentId() == null || request.getShipmentId().isBlank()) {
            return DeliveryResponse.error("shipmentId must not be null or empty"); // Changed to return error
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            return DeliveryResponse.error("location must not be null or empty"); // Changed to return error
        }

        // Retrieve from in-memory storage
        Optional<ShipmentEntity> optionalShipment = Optional.ofNullable(inMemoryStorage.get(request.getShipmentId()));
        if (optionalShipment.isEmpty()) {
            return DeliveryResponse.error("Shipment not found: " + request.getShipmentId());
        }

        ShipmentEntity shipment = optionalShipment.get();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setLastLocation(request.getLocation());
        shipment.setDeliveredAt(LocalDateTime.now());

        // Save updated shipment to in-memory storage
        inMemoryStorage.put(shipment.getShipmentId(), shipment);
        ShipmentEntity savedShipment = shipment; // In-memory, so just reference the updated object

        // Send event
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                savedShipment.getShipmentId(),
                savedShipment.getDestination(),
                savedShipment.getLastLocation(),
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );

        eventProducer.sendShipmentDeliveredEvent(event);
        logger.info("Shipment marked as delivered: {}", savedShipment.getShipmentId());

        return mapToDeliveryResponse(savedShipment);
    }

    @Override
    public Optional<ShipmentEntity> findShipmentEntityById(String shipmentId) {
        // Retrieve from in-memory storage (used by other components like listeners)
        return Optional.ofNullable(inMemoryStorage.get(shipmentId));
    }

    @Override
    // New public method for listeners to update/add shipment entities
    public void updateShipmentState(ShipmentEntity shipment) {
        if (shipment == null || shipment.getShipmentId() == null || shipment.getShipmentId().isBlank()) {
            logger.warn("Attempted to update shipment state with null or invalid shipment entity/ID.");
            return;
        }
        inMemoryStorage.put(shipment.getShipmentId(), shipment);
        logger.debug("Shipment {} state updated in in-memory storage.", shipment.getShipmentId());
    }


    private DeliveryResponse mapToDeliveryResponse(ShipmentEntity entity) {
        return new DeliveryResponse(
                entity.getShipmentId(),
                entity.getStatus(),
                entity.getLastLocation(),
                entity.getDeliveredAt()
        );
    }

    // Helper methods for testing and Kafka Listener to add/manage shipments in memory
    public void addShipmentForTest(ShipmentEntity shipment) {
        inMemoryStorage.put(shipment.getShipmentId(), shipment);
    }

    public void clearInMemoryStorageForTests() {
        inMemoryStorage.clear();
    }
}
