
package com.luckypets.logistics.scanservice.kafka;

import com.luckypets.logistics.scanservice.repository.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventListener.class);
    private final ShipmentRepository repository;

    public ShipmentEventListener(ShipmentRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "shipment-created", groupId = "scanservice")
    public void handleShipmentCreatedEvent(ShipmentCreatedEvent event) {
        logger.info("Empfangenes ShipmentCreatedEvent: {}", event);
        // Felder anpassen je nach Event!
        ShipmentEntity shipment = new ShipmentEntity(
                event.getShipmentId(),
                event.getDestination(),
                event.getCreatedAt()
        );
        repository.save(shipment);
    }
}
