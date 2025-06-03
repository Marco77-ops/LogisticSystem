package com.luckypets.logistics.deliveryservice.service;

import com.luckypets.logistics.deliveryservice.kafka.ShipmentDeliveredEventProducer;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the DeliveryService interface.
 */
@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);
    private final ShipmentRepository repository;
    private final ShipmentDeliveredEventProducer eventProducer;

    public DeliveryServiceImpl(ShipmentRepository repository, ShipmentDeliveredEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    @Override
    public List<DeliveryResponse> getAllShipments() {
        return repository.findAll().stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeliveryResponse> getShipmentById(String shipmentId) {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be null or empty");
        }
        return repository.findById(shipmentId)
                .map(this::mapToDeliveryResponse);
    }

    @Override
    public String getShipmentStatus(String shipmentId) {
        return repository.findById(shipmentId)
                .map(shipment -> shipment.getStatus().name())
                .orElse("Unknown");
    }

    @Override
    public DeliveryResponse markAsDelivered(DeliveryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getShipmentId() == null || request.getShipmentId().isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be null or empty");
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw new IllegalArgumentException("location must not be null or empty");
        }

        Optional<ShipmentEntity> optionalShipment = repository.findById(request.getShipmentId());
        if (optionalShipment.isEmpty()) {
            return DeliveryResponse.error("Shipment not found: " + request.getShipmentId());
        }

        ShipmentEntity shipment = optionalShipment.get();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setLastLocation(request.getLocation());
        shipment.setDeliveredAt(LocalDateTime.now());
        
        ShipmentEntity savedShipment = repository.save(shipment);
        
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
        return repository.findById(shipmentId);
    }

    private DeliveryResponse mapToDeliveryResponse(ShipmentEntity entity) {
        return new DeliveryResponse(
                entity.getShipmentId(),
                entity.getStatus(),
                entity.getLastLocation(),
                entity.getDeliveredAt()
        );
    }
}