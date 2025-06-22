package com.luckypets.logistics.scanservice.kafka;

import com.luckypets.logistics.scanservice.model.ShipmentEntity;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl;
import com.luckypets.logistics.shared.model.ShipmentStatus; // Import ShipmentStatus
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventListener.class);
    private final ScanServiceImpl scanService;

    public ShipmentEventListener(ScanServiceImpl scanService) {
        this.scanService = scanService;
    }

    @KafkaListener(topics = "shipment-created", groupId = "scanservice")
    public void handleShipmentCreatedEvent(ShipmentCreatedEvent event, Acknowledgment acknowledgment) {
        logger.info("Empfangenes ShipmentCreatedEvent: {}", event);

        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId(event.getShipmentId());
        shipment.setDestination(event.getDestination());
        shipment.setCreatedAt(event.getCreatedAt());
        // Populate origin and customerId if available in ShipmentCreatedEvent, otherwise null
        // Assuming ShipmentCreatedEvent doesn't contain origin/customerId currently,
        // so they will remain null in scanservice.model.ShipmentEntity.
        // If your ShipmentCreatedEvent does contain them, retrieve them here:
        // shipment.setOrigin(event.getOrigin());
        // shipment.setCustomerId(event.getCustomerId());

        // --- FIX START ---
        // Set the initial status to CREATED for the ScanService's in-memory representation.
        shipment.setStatus(ShipmentStatus.CREATED);
        // --- FIX END ---

        scanService.addShipmentForTest(shipment);

        logger.info("Shipment with ID {} added to ScanService in-memory storage.", shipment.getShipmentId());

        acknowledgment.acknowledge();
    }
}