package com.luckypets.logistics.scanservice.kafka;

import com.luckypets.logistics.scanservice.model.ShipmentEntity;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl; // Import ScanServiceImpl
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventListener.class);
    // Replace ShipmentRepository with ScanServiceImpl to manage in-memory shipments
    private final ScanServiceImpl scanService;

    // Update constructor to take ScanServiceImpl
    public ShipmentEventListener(ScanServiceImpl scanService) {
        this.scanService = scanService;
    }

    @KafkaListener(topics = "shipment-created", groupId = "scanservice")
    public void handleShipmentCreatedEvent(ShipmentCreatedEvent event) {
        logger.info("Empfangenes ShipmentCreatedEvent: {}", event);

        // Create ShipmentEntity from shared model, as it will be stored in ScanService's in-memory map
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentId(event.getShipmentId());
        shipment.setDestination(event.getDestination());
        // Assuming your shared.model.ShipmentEntity has a createdAt field and you want to set it
        shipment.setCreatedAt(event.getCreatedAt());
        // For other fields like origin, customerId, lastLocation, lastScannedAt, deliveredAt,
        // you might need to decide how to initialize them for the scan service's purposes.
        // For now, we'll keep it minimal based on the event.
        // Also, for scan service, initially lastLocation and lastScannedAt would be null
        // or derived from the event if it contains initial location data.
        // Since scanservice's ShipmentEntity also had a lastLocation, lastScannedAt,
        // and destination, we should populate them from the event or based on logic.
        // In this case, the ShipmentCreatedEvent only has ShipmentId, Destination, CreatedAt.
        // Let's ensure the shared ShipmentEntity can accommodate these.

        // Add the created shipment to the ScanService's in-memory storage
        // This makes the shipment available for scanning operations in the ScanService.
        scanService.addShipmentForTest(shipment); // Using the helper method from ScanServiceImpl

        logger.info("Shipment with ID {} added to ScanService in-memory storage.", shipment.getShipmentId());
    }
}
