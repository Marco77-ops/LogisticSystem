package com.luckypets.logistics.deliveryservice.kafka;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ShipmentDeliveredEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentDeliveredEventProducer.class);
    private final KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    public ShipmentDeliveredEventProducer(KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendShipmentDeliveredEvent(ShipmentDeliveredEvent event) {
        logger.info("Sende ShipmentDeliveredEvent: {}", event);

        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future =
                kafkaTemplate.send("shipment-delivered", event.getShipmentId(), event);

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
