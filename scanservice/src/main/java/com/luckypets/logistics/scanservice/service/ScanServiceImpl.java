package com.luckypets.logistics.scanservice.service;

import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the ScanService interface.
 * Handles the business logic for processing shipment scans.
 */
@Service
public class ScanServiceImpl implements ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanServiceImpl.class);
    private static final String TOPIC_SHIPMENT_SCANNED = "shipment-scanned";

    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;

    public ScanServiceImpl(ShipmentRepository shipmentRepository,
                          KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate) {
        this.shipmentRepository = shipmentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public ScanResult scanShipment(String shipmentId, String location) {
        // Validate input parameters
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID cannot be null");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }

        // Find the shipment
        Optional<ShipmentEntity> optionalShipment = shipmentRepository.findById(shipmentId);
        if (optionalShipment.isEmpty()) {
            logger.warn("Shipment not found: {}", shipmentId);
            return ScanResult.failure("Shipment not found");
        }

        ShipmentEntity shipment = optionalShipment.get();
        
        // Update shipment with scan information
        updateShipmentWithScan(shipment, location);
        
        // Save the updated shipment
        shipmentRepository.save(shipment);
        
        // Create and send event
        ShipmentScannedEvent event = createShipmentScannedEvent(shipment, location);
        sendShipmentScannedEvent(event);
        
        logger.info("Shipment scanned successfully: {} at {}", shipmentId, location);
        return ScanResult.success(shipmentId);
    }
    
    /**
     * Updates the shipment entity with scan information.
     * 
     * @param shipment the shipment to update
     * @param location the location where the shipment was scanned
     */
    private void updateShipmentWithScan(ShipmentEntity shipment, String location) {
        // Update last location
        shipment.setLastLocation(location);
        
        // Update last scanned timestamp
        shipment.setLastScannedAt(LocalDateTime.now());
        
        // Update status based on scan context
        updateShipmentStatus(shipment, location);
    }
    
    /**
     * Updates the shipment status based on the scan context.
     * 
     * @param shipment the shipment to update
     * @param location the location where the shipment was scanned
     */
    private void updateShipmentStatus(ShipmentEntity shipment, String location) {
        // If the shipment is at the destination, mark it as delivered
        if (location.equals(shipment.getDestination())) {
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setDeliveredAt(LocalDateTime.now());
            logger.info("Shipment {} marked as DELIVERED at destination {}", 
                    shipment.getShipmentId(), location);
        } 
        // Otherwise, mark it as in transit if it's not already delivered
        else if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            logger.info("Shipment {} marked as IN_TRANSIT at {}", 
                    shipment.getShipmentId(), location);
        }
    }
    
    /**
     * Creates a ShipmentScannedEvent for the given shipment and location.
     * 
     * @param shipment the shipment that was scanned
     * @param location the location where the shipment was scanned
     * @return a new ShipmentScannedEvent
     */
    private ShipmentScannedEvent createShipmentScannedEvent(ShipmentEntity shipment, String location) {
        return new ShipmentScannedEvent(
                shipment.getShipmentId(),
                location,
                LocalDateTime.now(),
                shipment.getDestination(),
                generateCorrelationId()
        );
    }
    
    /**
     * Sends a ShipmentScannedEvent to Kafka.
     * 
     * @param event the event to send
     */
    private void sendShipmentScannedEvent(ShipmentScannedEvent event) {
        logger.info("Sending ShipmentScannedEvent: {}", event);
        kafkaTemplate.send(TOPIC_SHIPMENT_SCANNED, event.getShipmentId(), event);
    }
    
    /**
     * Generates a unique correlation ID for events.
     * 
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}