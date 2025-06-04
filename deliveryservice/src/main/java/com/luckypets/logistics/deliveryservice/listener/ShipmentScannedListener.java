package com.luckypets.logistics.deliveryservice.listener;

import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@Service
public class ShipmentScannedListener {

    private static final Logger log = LoggerFactory.getLogger(ShipmentScannedListener.class);

    private final ShipmentRepository repository;
    private final KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @Value("${kafka.topic.delivered:shipment-delivered}")  // Property sauber injiziert!
    private String deliveredTopic;

    public ShipmentScannedListener(
            ShipmentRepository repository,
            KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topic.scanned:shipment-scanned}")
    public void onShipmentScanned(ShipmentScannedEvent event) {
        log.info("Received scan event: {}", event);

        // 1) Lade vorhandenes Shipment oder lege neues an
        ShipmentEntity entity = repository.findById(event.getShipmentId())
                .orElseGet(() -> {
                    ShipmentEntity ne = new ShipmentEntity();
                    ne.setShipmentId(event.getShipmentId());
                    ne.setDestination(event.getDestination());
                    ne.setCreatedAt(LocalDateTime.now());
                    return ne;
                });

        // 2) Update letzte Location + Zeit + Status
        entity.setLastLocation(event.getLocation());
        entity.setLastScannedAt(event.getScannedAt());
        entity.setStatus(ShipmentStatus.IN_TRANSIT);

        // 3) Wenn Ziel erreicht → Delivered-Event
        if (event.getLocation().equalsIgnoreCase(event.getDestination())) {
            entity.setStatus(ShipmentStatus.DELIVERED);
            entity.setDeliveredAt(LocalDateTime.now());

            // push Delivered-Event
            ShipmentDeliveredEvent delivered = new ShipmentDeliveredEvent(
                    event.getShipmentId(),
                    event.getDestination(),
                    event.getLocation(),
                    LocalDateTime.now(),
                    event.getCorrelationId()
            );
            try {
                SendResult<String, ShipmentDeliveredEvent> result =
                        kafkaTemplate.send(deliveredTopic, delivered.getShipmentId(), delivered)
                                .get();
                log.info(" DeliveredEvent sent (partition={}, offset={})",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (InterruptedException | ExecutionException ex) {
                log.error(" Failed to send DeliveredEvent", ex);
                Thread.currentThread().interrupt();
            }
        }

        // 4) Speichern in der Datenbank
        repository.save(entity);
    }
}
