package com.luckypets.logistics.shipmentservice.service;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shipmentservice.persistence.ShipmentStatus; // Import für ShipmentStatus
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Für @Transactional

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Für die Generierung einer ID

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventProducer eventProducer;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository, ShipmentEventProducer eventProducer) {
        this.shipmentRepository = shipmentRepository;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public ShipmentEntity createShipment(ShipmentRequest request) {
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId(UUID.randomUUID().toString().substring(0, 8)); // Beispiel ID
        entity.setOrigin(request.getOrigin());
        entity.setDestination(request.getDestination());
        entity.setCustomerId(request.getCustomerId());
        entity.setStatus(ShipmentStatus.CREATED); // Setze den initialen Status
        entity.setCreatedAt(LocalDateTime.now());
        // Setzen Sie hier ggf. weitere Standardwerte
        ShipmentEntity savedEntity = shipmentRepository.save(entity);

        // Create and publish ShipmentCreatedEvent
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
            savedEntity.getShipmentId(),
            savedEntity.getDestination(),
            savedEntity.getCreatedAt(),
            UUID.randomUUID().toString() // Generate a correlation ID
        );
        eventProducer.sendShipmentCreatedEvent(event);

        return savedEntity;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentEntity> getShipmentById(String shipmentId) {
        return shipmentRepository.findById(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentEntity> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Override
    @Transactional
    public boolean deleteShipment(String shipmentId) {
        if (shipmentRepository.existsById(shipmentId)) {
            shipmentRepository.deleteById(shipmentId);
            return true;
        }
        return false;
    }
}
