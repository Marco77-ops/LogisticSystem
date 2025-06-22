package com.luckypets.logistics.deliveryservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    /**
     * Defines a custom error handler for Kafka listeners.
     * This bean is referenced by name in @KafkaListener(errorHandler = "kafkaListenerErrorHandler").
     * It logs the failed message and the exception, then re-throws the exception
     * to allow Spring Kafka's default retry/DLQ mechanisms to take over if configured.
     *
     * @return A KafkaListenerErrorHandler bean.
     */
    @Bean
    public KafkaListenerErrorHandler kafkaListenerErrorHandler() {
        return (Message<?> message, ListenerExecutionFailedException exception) -> {
            log.error("Kafka Message processing failed for message: {}. Original error: {}", message.getPayload(), exception.getMessage(), exception);
            // Re-throw the exception to ensure that the Kafka container's error handling
            // (e.g., retries, dead-letter queue) is applied.
            throw exception;
        };
    }
}