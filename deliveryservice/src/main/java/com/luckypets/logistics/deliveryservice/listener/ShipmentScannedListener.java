package com.luckypets.logistics.deliveryservice.listener;

import com.luckypets.logistics.deliveryservice.model.ShipmentEntity; // Import local ShipmentEntity
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl; // Import DeliveryServiceImpl for in-memory access
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

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@Service
public class ShipmentScannedListener {

    private static final Logger log = LoggerFactory.getLogger(ShipmentScannedListener.class);

    // Replace ShipmentRepository with DeliveryServiceImpl for in-memory access
    private final DeliveryServiceImpl deliveryService;
    private final KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @Value("${kafka.topic.delivered:shipment-delivered}")
    private String deliveredTopic;

    public ShipmentScannedListener(
            DeliveryServiceImpl deliveryService, // Inject DeliveryServiceImpl
            KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate
    ) {
        this.deliveryService = deliveryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topic.scanned:shipment-scanned}")
    public void onShipmentScanned(ShipmentScannedEvent event, Acknowledgment acknowledgment) {
        log.info("Received scan event: {}", event);

        // Retrieve from in-memory storage or create a new entity
        ShipmentEntity entity = deliveryService.findShipmentEntityById(event.getShipmentId())
                .orElseGet(() -> {
                    // Initialize with data from ShipmentScannedEvent
                    ShipmentEntity newEntity = new ShipmentEntity(
                            event.getShipmentId(),
                            null, // Origin not available in ShipmentScannedEvent
                            event.getDestination(),
                            null, // CustomerId not available in ShipmentScannedEvent
                            LocalDateTime.now(), // Set creation time here
                            ShipmentStatus.CREATED // Initial status
                    );
                    return newEntity;
                });

        entity.setLastLocation(event.getLocation());
        entity.setLastScannedAt(event.getScannedAt()); // Use the timestamp from the event

        // Only change status to IN_TRANSIT if it's not already DELIVERED
        if (entity.getStatus() != ShipmentStatus.DELIVERED) {
            entity.setStatus(ShipmentStatus.IN_TRANSIT);
        }

        // Check for delivery condition
        if (event.getLocation().equalsIgnoreCase(entity.getDestination())) {
            entity.setStatus(ShipmentStatus.DELIVERED);
            entity.setDeliveredAt(LocalDateTime.now());

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
                log.info("DeliveredEvent sent (partition={}, offset={})",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Failed to send DeliveredEvent", ex);
                // Restore interrupt status
                Thread.currentThread().interrupt();
            }
        }

        // Save updated entity to in-memory storage via service method
        deliveryService.updateShipmentState(entity); // Use the new updateShipmentState method

        // Acknowledge the message
        acknowledgment.acknowledge();
    }
}
