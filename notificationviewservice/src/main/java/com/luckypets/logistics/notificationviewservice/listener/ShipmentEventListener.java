package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.notificationviewservice.service.ServerlessNotificationService;
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
    private final ServerlessNotificationService serverlessService;

    public ShipmentEventListener(NotificationService service, ServerlessNotificationService serverlessService) {
        this.service = service;
        this.serverlessService = serverlessService;
        logger.info("ShipmentEventListener initialized with Serverless support");
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
        logger.info("📦 Event details: shipmentId={}, destination={}",
                event != null ? event.getShipmentId() : "null",
                event != null ? event.getDestination() : "null");

        if (event == null || event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.warn("⚠️ Received invalid ShipmentCreatedEvent - skipping");
            acknowledgment.acknowledge();
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    generateShipmentCreatedMessage(event),
                    NotificationType.SHIPMENT_CREATED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Notification created: id={}, shipmentId={}", saved.getId(), saved.getShipmentId());


            serverlessService.triggerServerlessFunction(
                    "shipment-created",
                    event.getShipmentId(),
                    null,
                    null,
                    event.getDestination()
            );

            acknowledgment.acknowledge();
            logger.info("📨 Event processed successfully");

        } catch (Exception e) {
            logger.error("❌ Error processing ShipmentCreatedEvent: shipmentId={}", event.getShipmentId(), e);
            throw e;
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
        logger.info("📍 Event details: shipmentId={}, location={}, destination={}",
                event != null ? event.getShipmentId() : "null",
                event != null ? event.getLocation() : "null",
                event != null ? event.getDestination() : "null");

        if (event == null || event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.warn("⚠️ Received invalid ShipmentScannedEvent - skipping");
            acknowledgment.acknowledge();
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    generateShipmentScannedMessage(event),
                    NotificationType.SHIPMENT_SCANNED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Notification created: id={}, shipmentId={}", saved.getId(), saved.getShipmentId());


            serverlessService.triggerServerlessFunction(
                    "shipment-scanned",
                    event.getShipmentId(),
                    null,
                    event.getLocation(),
                    event.getDestination()
            );

            acknowledgment.acknowledge();
            logger.info("📨 Event processed successfully");

        } catch (Exception e) {
            logger.error("❌ Error processing ShipmentScannedEvent: shipmentId={}", event.getShipmentId(), e);
            throw e;
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
        logger.info("🚚 Event details: shipmentId={}, location={}, destination={}",
                event != null ? event.getShipmentId() : "null",
                event != null ? event.getLocation() : "null",
                event != null ? event.getDestination() : "null");

        if (event == null || event.getShipmentId() == null || event.getShipmentId().trim().isEmpty()) {
            logger.warn("⚠️ Received invalid ShipmentDeliveredEvent - skipping");
            acknowledgment.acknowledge();
            return;
        }

        try {
            Notification notification = new Notification(
                    event.getShipmentId(),
                    generateShipmentDeliveredMessage(event),
                    NotificationType.SHIPMENT_DELIVERED
            );

            Notification saved = service.save(notification);
            logger.info("✅ Notification created: id={}, shipmentId={}", saved.getId(), saved.getShipmentId());


            serverlessService.triggerServerlessFunction(
                    "shipment-delivered",
                    event.getShipmentId(),
                    null,
                    null,
                    event.getLocation()
            );

            acknowledgment.acknowledge();
            logger.info("📨 Event processed successfully");

        } catch (Exception e) {
            logger.error("❌ Error processing ShipmentDeliveredEvent: shipmentId={}", event.getShipmentId(), e);
            throw e;
        }
    }

    private String generateShipmentCreatedMessage(ShipmentCreatedEvent event) {
        return String.format("Shipment %s has been created and is being prepared for transport to %s.",
                event.getShipmentId(), event.getDestination());
    }

    private String generateShipmentScannedMessage(ShipmentScannedEvent event) {
        return String.format("Shipment %s has been scanned at %s. Current destination: %s.",
                event.getShipmentId(), event.getLocation(), event.getDestination());
    }

    private String generateShipmentDeliveredMessage(ShipmentDeliveredEvent event) {
        return String.format("Shipment %s has been successfully delivered to %s.",
                event.getShipmentId(), event.getLocation());
    }
}