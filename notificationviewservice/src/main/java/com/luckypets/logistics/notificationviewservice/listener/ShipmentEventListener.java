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
        logger.info("ShipmentEventListener initialized");
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.shipment-created:shipment-created}",
            groupId = "${spring.kafka.consumer.group-id:notification-view-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleShipmentCreated(
            @Payload ShipmentCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("🎯 RECEIVED ShipmentCreatedEvent from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        logger.info("📦 Event details: {}", event);

        // Validation mit sofortigem Acknowledge bei Invalid Data
        if (event == null) {
            logger.error("❌ Received null event for shipment created");
            acknowledgment.acknowledge();
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("❌ Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge();
            return;
        }

        try {
            logger.info("🔄 Processing ShipmentCreatedEvent for shipment: {}", event.getShipmentId());

            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been created with destination %s",
                            event.getShipmentId(), event.getDestination()),
                    NotificationType.SHIPMENT_CREATED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Successfully saved notification: {}", saved.getId());

            // Debug: Log current notification count
            long totalCount = service.getNotificationCount();
            logger.info("📊 Total notifications in system: {}", totalCount);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("❌ Error processing shipment created event: {}", event, e);
            // Nicht acknowledgen - lass den Error Handler das DLQ Pattern handhaben
            throw new RuntimeException("Failed to process shipment created event", e);
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.shipment-scanned:shipment-scanned}",
            groupId = "${spring.kafka.consumer.group-id:notification-view-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleShipmentScanned(
            @Payload ShipmentScannedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("🎯 RECEIVED ShipmentScannedEvent from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        logger.info("📍 Event details: {}", event);

        if (event == null) {
            logger.error("❌ Received null event for shipment scanned");
            acknowledgment.acknowledge();
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("❌ Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge();
            return;
        }

        try {
            logger.info("🔄 Processing ShipmentScannedEvent for shipment: {}", event.getShipmentId());

            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been scanned at location %s",
                            event.getShipmentId(), event.getLocation()),
                    NotificationType.SHIPMENT_SCANNED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Successfully saved notification: {}", saved.getId());

            long totalCount = service.getNotificationCount();
            logger.info("📊 Total notifications in system: {}", totalCount);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("❌ Error processing shipment scanned event: {}", event, e);
            throw new RuntimeException("Failed to process shipment scanned event", e);
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.shipment-delivered:shipment-delivered}",
            groupId = "${spring.kafka.consumer.group-id:notification-view-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleShipmentDelivered(
            @Payload ShipmentDeliveredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("🎯 RECEIVED ShipmentDeliveredEvent from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        logger.info("🚚 Event details: {}", event);

        if (event == null) {
            logger.error("❌ Received null event for shipment delivered");
            acknowledgment.acknowledge();
            return;
        }

        if (event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.error("❌ Received event with null or empty shipmentId: {}", event);
            acknowledgment.acknowledge();
            return;
        }

        try {
            logger.info("🔄 Processing ShipmentDeliveredEvent for shipment: {}", event.getShipmentId());

            Notification notification = new Notification(
                    event.getShipmentId(),
                    String.format("Shipment %s has been delivered to its destination %s",
                            event.getShipmentId(), event.getDestination()),
                    NotificationType.SHIPMENT_DELIVERED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Successfully saved notification: {}", saved.getId());

            long totalCount = service.getNotificationCount();
            logger.info("📊 Total notifications in system: {}", totalCount);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("❌ Error processing shipment delivered event: {}", event, e);
            throw new RuntimeException("Failed to process shipment delivered event", e);
        }
    }
}