package com.luckypets.logistics.notificationviewservice.kafka;

import com.luckypets.logistics.shared.events.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationSentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSentEventProducer.class);

    private final KafkaTemplate<String, NotificationSentEvent> kafkaTemplate;
    private final String topic;

    public NotificationSentEventProducer(
            KafkaTemplate<String, NotificationSentEvent> kafkaTemplate,
            @Value("${spring.kafka.topic.notification-sent}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        logger.info("NotificationSentEventProducer initialized with topic: {}", topic);
    }

    public void sendNotificationSentEvent(NotificationSentEvent event) {
        try {
            logger.info("Sending event to topic '{}': {}", topic, event);
            kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send event to topic '{}': {}", topic, event, ex);
                } else {
                    logger.info("Successfully sent event to topic '{}': {}", topic, event);
                }
            });
        } catch (Exception e) {
            logger.error("Exception while sending event to topic '{}': {}", topic, event, e);
            throw e;
        }
    }
}