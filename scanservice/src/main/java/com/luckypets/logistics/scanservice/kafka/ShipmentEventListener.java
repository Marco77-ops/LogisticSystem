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

        shipment.setStatus(ShipmentStatus.CREATED);


        scanService.addShipmentForTest(shipment);

        logger.info("Shipment with ID {} added to ScanService in-memory storage.", shipment.getShipmentId());

        acknowledgment.acknowledge();
    }
}