package com.luckypets.logistics.deliveryservice.unittest.kafka;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;

public class ShipmentDeliveredEventProducerTest {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentDeliveredEventProducerTest.class);
    private static final String TOPIC = "shipment-delivered-v2";

    private final KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    public ShipmentDeliveredEventProducerTest(KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendShipmentDeliveredEvent(ShipmentDeliveredEvent event) {
        logger.info("Sende ShipmentDeliveredEvent: {}", event);

        Message<ShipmentDeliveredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader(KafkaHeaders.KEY, event.getShipmentId())
                .build();

        // Sende Message an Kafka, und reagiere auf das Future korrekt (defensiv gegen null)
        var future = kafkaTemplate.send(message);

        if (future != null) {
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send event to Kafka", ex);
                } else {
                    logger.info("Event sent successfully to Kafka topic: {}", TOPIC);
                }
            });
        } else {
            logger.warn("KafkaTemplate.send() returned null Future! Event could not be sent: {}", event);
        }
    }
}
