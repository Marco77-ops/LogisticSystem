package com.luckypets.logistics.scanservice.service;

import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.model.ShipmentEntity;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanServiceImpl implements ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanServiceImpl.class);
    private static final String TOPIC_SHIPMENT_SCANNED = "shipment-scanned";

    private final ConcurrentHashMap<String, ShipmentEntity> inMemoryStorage = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    public ScanServiceImpl(KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public ScanResult scanShipment(String shipmentId, String location) {
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }

        Optional<ShipmentEntity> optionalShipment = Optional.ofNullable(inMemoryStorage.get(shipmentId));

        if (optionalShipment.isEmpty()) {
            logger.warn("Shipment not found: {}", shipmentId);
            return ScanResult.failure("Shipment with ID " + shipmentId + " not found.");
        }

        ShipmentEntity shipment = optionalShipment.get();

        // Update shipment with scan information
        shipment.setLastLocation(location);
        shipment.setLastScannedAt(LocalDateTime.now());
        // Update status if it's currently not DELIVERED
        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        }

        // Update in-memory storage
        inMemoryStorage.put(shipment.getShipmentId(), shipment);

        // Create and send event
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                shipment.getShipmentId(),
                location,
                LocalDateTime.now(),
                shipment.getDestination(),
                UUID.randomUUID().toString()
        );
        kafkaTemplate.send(TOPIC_SHIPMENT_SCANNED, event.getShipmentId(), event);

        logger.info("Shipment scanned successfully: {} at {}", shipmentId, location);
        return ScanResult.success(shipmentId);
    }


    public void addShipmentForTest(ShipmentEntity shipment) {
        inMemoryStorage.put(shipment.getShipmentId(), shipment);
    }
    public void clearInMemoryStorageForTests() {
        inMemoryStorage.clear();
    }
    public Optional<ShipmentEntity> findById(String shipmentId) {
        return Optional.ofNullable(inMemoryStorage.get(shipmentId));
    }
}
