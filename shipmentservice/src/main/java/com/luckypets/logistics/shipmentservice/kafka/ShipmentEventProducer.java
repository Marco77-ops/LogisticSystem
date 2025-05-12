package com.luckypets.logistics.shipmentservice.kafka;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ShipmentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventProducer.class);
    private final KafkaTemplate<String, ShipmentCreatedEvent> kafkaTemplate;

    public ShipmentEventProducer(KafkaTemplate<String, ShipmentCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendShipmentCreatedEvent(ShipmentCreatedEvent event) {
        logger.info("Sende ShipmentCreatedEvent: {}", event);

        CompletableFuture<SendResult<String, ShipmentCreatedEvent>> future =
                kafkaTemplate.send("shipment-created", event.getShipmentId(), event);

        future.thenAccept(result -> {
            RecordMetadata metadata = result.getRecordMetadata();
            logger.info("Kafka send success: topic={}, partition={}, offset={}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        }).exceptionally(ex -> {
            logger.error("Kafka send failed for shipmentId={}", event.getShipmentId(), ex);
            return null;
        });
    }
}
