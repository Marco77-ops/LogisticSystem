package com.luckypets.logistics.deliveryservice.listener;

import com.luckypets.logistics.deliveryservice.model.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.messaging.handler.annotation.Payload; // Import @Payload
import org.springframework.messaging.handler.annotation.Header;   // Import @Header
import org.springframework.kafka.support.KafkaHeaders;         // Import KafkaHeaders


import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@Service
public class ShipmentScannedListener {

    private static final Logger log = LoggerFactory.getLogger(ShipmentScannedListener.class);

    private final DeliveryServiceImpl deliveryService;
    private final KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @Value("${kafka.topic.delivered:shipment-delivered}")
    private String deliveredTopic;

    public ShipmentScannedListener(
            DeliveryServiceImpl deliveryService,
            KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate
    ) {
        this.deliveryService = deliveryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topic.scanned:shipment-scanned}", errorHandler = "kafkaListenerErrorHandler")
    public void onShipmentScanned(
            @Payload ShipmentScannedEvent event, // Explicitly mark this as the message payload
            @Header(KafkaHeaders.ACKNOWLEDGMENT) Acknowledgment acknowledgment // Explicitly mark this as the Acknowledgment header
    ) {
        log.info("Received scan event for shipmentId: {}. Event details: {}", event.getShipmentId(), event);

        // Basic null checks for crucial event data
        if (event.getShipmentId() == null || event.getShipmentId().isBlank()) {
            log.warn("Received ShipmentScannedEvent with null or empty shipmentId. Skipping processing: {}", event);
            acknowledgment.acknowledge(); // Acknowledge to prevent reprocessing this bad message
            return;
        }

        ShipmentEntity entity;
        // Retrieve from in-memory storage or create a new entity
        if (deliveryService.findShipmentEntityById(event.getShipmentId()).isPresent()) {
            entity = deliveryService.findShipmentEntityById(event.getShipmentId()).get();
            log.info("Found existing ShipmentEntity in DeliveryService for ID: {}. Current Status: {}", entity.getShipmentId(), entity.getStatus());
        } else {
            // Initialize with data from ShipmentScannedEvent
            entity = new ShipmentEntity(
                    event.getShipmentId(),
                    null, // Origin not available in ShipmentScannedEvent
                    event.getDestination(), // Destination can be null from event
                    null, // CustomerId not available in ShipmentScannedEvent
                    LocalDateTime.now(), // Set creation time here (as it's the first time delivery service sees it)
                    ShipmentStatus.CREATED // Initial status
            );
            log.info("Creating new ShipmentEntity for DeliveryService with ID: {}. Initial Status: {}", entity.getShipmentId(), entity.getStatus());
        }

        entity.setLastLocation(event.getLocation());
        entity.setLastScannedAt(event.getScannedAt());
        log.info("ShipmentEntity {} updated with location: {} and scannedAt: {}", entity.getShipmentId(), entity.getLastLocation(), entity.getLastScannedAt());


        // Only change status to IN_TRANSIT if it's not already DELIVERED
        if (entity.getStatus() != ShipmentStatus.DELIVERED) {
            entity.setStatus(ShipmentStatus.IN_TRANSIT);
            log.info("Shipment {} status set to IN_TRANSIT.", entity.getShipmentId());
        } else {
            log.info("Shipment {} is already DELIVERED. Status not changed.", entity.getShipmentId());
        }

        // Check for delivery condition with robust null checks
        if (event.getLocation() != null && !event.getLocation().isBlank() &&
                entity.getDestination() != null && !entity.getDestination().isBlank() &&
                event.getLocation().equalsIgnoreCase(entity.getDestination())) {

            entity.setStatus(ShipmentStatus.DELIVERED);
            entity.setDeliveredAt(LocalDateTime.now());
            log.info("Shipment {} delivered! Location: '{}', Destination: '{}'. Status set to DELIVERED.",
                    entity.getShipmentId(), entity.getLastLocation(), entity.getDestination());

            ShipmentDeliveredEvent delivered = new ShipmentDeliveredEvent(
                    event.getShipmentId(),
                    entity.getDestination(), // Use destination from entity
                    event.getLocation(),
                    LocalDateTime.now(),
                    event.getCorrelationId()
            );
            try {
                SendResult<String, ShipmentDeliveredEvent> result =
                        kafkaTemplate.send(deliveredTopic, delivered.getShipmentId(), delivered).get();
                log.info("DeliveredEvent sent for shipment {}: topic={}, partition={}, offset={}",
                        delivered.getShipmentId(), result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Failed to send DeliveredEvent for shipment {}. Error: {}", event.getShipmentId(), ex.getMessage(), ex);
                // Restore interrupt status
                Thread.currentThread().interrupt();
            }
        } else {
            log.debug("Shipment {} not delivered yet. Current location: '{}', Destination: '{}'. Status: {}",
                    event.getShipmentId(), event.getLocation(), entity.getDestination(), entity.getStatus());
        }

        // Save updated entity to in-memory storage via service method
        deliveryService.updateShipmentState(entity);
        log.info("Shipment {} updated/added to DeliveryService in-memory storage. Final status in listener: {}", entity.getShipmentId(), entity.getStatus());

        // Acknowledge the message
        acknowledgment.acknowledge();
        log.info("Acknowledgment sent for shipment {}", event.getShipmentId());
    }
}
