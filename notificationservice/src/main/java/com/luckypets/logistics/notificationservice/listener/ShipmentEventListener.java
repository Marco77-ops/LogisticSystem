package com.luckypets.logistics.notificationservice.listener;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.model.NotificationType;
import com.luckypets.logistics.notificationservice.repository.NotificationRepository;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ShipmentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventListener.class);
    private final NotificationRepository repository;

    public ShipmentEventListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "shipment-created", groupId = "notification-service")
    public void handleShipmentCreated(ShipmentCreatedEvent event) {
        logger.info("Received shipment created event: {}", event);
        
        Notification notification = new Notification(
                event.getShipmentId(),
                String.format("Shipment %s has been created with destination %s", 
                        event.getShipmentId(), event.getDestination()),
                NotificationType.SHIPMENT_CREATED
        );
        
        repository.save(notification);
        logger.info("Saved notification: {}", notification);
    }

    @KafkaListener(topics = "shipment-scanned", groupId = "notification-service")
    public void handleShipmentScanned(ShipmentScannedEvent event) {
        logger.info("Received shipment scanned event: {}", event);
        
        Notification notification = new Notification(
                event.getShipmentId(),
                String.format("Shipment %s has been scanned at location %s", 
                        event.getShipmentId(), event.getLocation()),
                NotificationType.SHIPMENT_SCANNED
        );
        
        repository.save(notification);
        logger.info("Saved notification: {}", notification);
    }

    @KafkaListener(topics = "shipment-delivered", groupId = "notification-service")
    public void handleShipmentDelivered(ShipmentDeliveredEvent event) {
        logger.info("Received shipment delivered event: {}", event);
        
        Notification notification = new Notification(
                event.getShipmentId(),
                String.format("Shipment %s has been delivered to %s", 
                        event.getShipmentId(), event.getLocation()),
                NotificationType.SHIPMENT_DELIVERED
        );
        
        repository.save(notification);
        logger.info("Saved notification: {}", notification);
    }
}