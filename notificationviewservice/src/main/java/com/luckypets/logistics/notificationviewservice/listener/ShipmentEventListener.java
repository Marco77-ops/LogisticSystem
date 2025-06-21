package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class ShipmentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventListener.class);
    private final NotificationService service;

    public ShipmentEventListener(NotificationService service) {
        this.service = service;
    }

    @KafkaListener(topics = "shipment-created", groupId = "notification-view-service")
    public void handleShipmentCreated(
            @Payload ShipmentCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received shipment created event from topic: {}, partition: {}, offset: {}, event: {}",
                topic, partition, offset, event);

        // Validation mit sofortigem Acknowledge bei Invalid Data
        if (event == null) {
            logger.error("Received null event for shipment created");
            acknowledgment.acknowledge(); // OK - Invalid Data soll nicht retried werden
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge(); // OK - Invalid Data soll nicht retried werden
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been created with destination %s",
                            event.getShipmentId(), event.getDestination()),
                    NotificationType.SHIPMENT_CREATED
            );

            service.save(notification);
            logger.info("Successfully saved notification: {}", notification);
            acknowledgment.acknowledge(); // ✅ Nur bei Erfolg acknowledgen

        } catch (Exception e) {
            logger.error("Error processing shipment created event: {}", event, e);
            // ✅ NICHT acknowledgen - lass den Error Handler das DLQ Pattern handhaben
            throw new RuntimeException("Failed to process shipment created event", e);
        }
    }

    @KafkaListener(topics = "shipment-scanned", groupId = "notification-view-service")
    public void handleShipmentScanned(
            @Payload ShipmentScannedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received shipment scanned event from topic: {}, partition: {}, offset: {}, event: {}",
                topic, partition, offset, event);

        if (event == null) {
            logger.error("Received null event for shipment scanned");
            acknowledgment.acknowledge();
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge();
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been scanned at location %s",
                            event.getShipmentId(), event.getLocation()),
                    NotificationType.SHIPMENT_SCANNED
            );

            service.save(notification);
            logger.info("Successfully saved notification: {}", notification);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing shipment scanned event: {}", event, e);
            throw new RuntimeException("Failed to process shipment scanned event", e);
        }
    }

    @KafkaListener(topics = "shipment-delivered", groupId = "notification-view-service")
    public void handleShipmentDelivered(
            @Payload ShipmentDeliveredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received shipment delivered event from topic: {}, partition: {}, offset: {}, event: {}",
                topic, partition, offset, event);

        if (event == null) {
            logger.error("Received null event for shipment delivered");
            acknowledgment.acknowledge();
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge();
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been delivered to its destination %s",
                            event.getShipmentId(), event.getDestination()),
                    NotificationType.SHIPMENT_DELIVERED
            );

            service.save(notification);
            logger.info("Successfully saved notification: {}", notification);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing shipment delivered event: {}", event, e);
            throw new RuntimeException("Failed to process shipment delivered event", e);
        }
    }
}