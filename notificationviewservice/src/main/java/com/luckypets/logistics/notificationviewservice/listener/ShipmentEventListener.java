package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
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

    @KafkaListener(topics = "shipment-created", groupId = "notificationView-service")
    public void handleShipmentCreated(
            @Payload ShipmentCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received shipment created event from topic: {}, partition: {}, offset: {}, event: {}",
                    topic, partition, offset, event);

            if (event == null) {
                logger.error("Received null event for shipment created");
                acknowledgment.acknowledge();
                return;
            }

            if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
                logger.error("Received event with null or empty shipmentId: {}", event);
                acknowledgment.acknowledge();
                return;
            }

            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been created with destination %s",
                            event.getShipmentId(), event.getDestination()),
                    NotificationType.SHIPMENT_CREATED
            );

            service.save(notification);
            logger.info("Successfully saved notification: {}", notification);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing shipment created event: {}", event, e);
            // Don't rethrow - acknowledge the message to avoid infinite retries
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = "shipment-scanned", groupId = "notificationView-service")
    public void handleShipmentScanned(
            @Payload ShipmentScannedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
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
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.shipment-delivered}", groupId = "notificationView-service")
    public void handleShipmentDelivered(
            @Payload ShipmentDeliveredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
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

            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been delivered to %s",
                            event.getShipmentId(), event.getLocation()),
                    NotificationType.SHIPMENT_DELIVERED
            );

            service.save(notification);
            logger.info("Successfully saved notification: {}", notification);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing shipment delivered event: {}", event, e);
            acknowledgment.acknowledge();
        }
    }
}