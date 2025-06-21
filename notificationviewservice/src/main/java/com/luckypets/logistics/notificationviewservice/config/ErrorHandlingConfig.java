package com.luckypets.logistics.notificationviewservice.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for error handling in Kafka consumers
 * Implements dead letter queue pattern for failed message processing
 */
@Configuration
public class ErrorHandlingConfig {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Producer factory for sending failed messages to dead letter topics
     */
    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for dead letter publishing
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate() {
        return new KafkaTemplate<>(deadLetterProducerFactory());
    }

    /**
     * Dead letter publishing recoverer - sends failed messages to DLT (Dead Letter Topic)
     * Failed messages from 'original-topic' will be sent to 'original-topic.DLT'
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate(),
                (consumerRecord, exception) -> {
                    // Log the failed message details
                    logger.error("Message processing failed for topic: {}, partition: {}, offset: {}, key: {}, exception: {}",
                            consumerRecord.topic(),
                            consumerRecord.partition(),
                            consumerRecord.offset(),
                            consumerRecord.key(),
                            exception.getMessage());

                    // Custom logic to determine dead letter topic name
                    String deadLetterTopic = consumerRecord.topic() + ".DLT";
                    logger.info("Sending failed message to dead letter topic: {}", deadLetterTopic);

                    // Return TopicPartition with the same partition as the original record
                    return new TopicPartition(deadLetterTopic, consumerRecord.partition());
                });
    }

    /**
     * Error handler with retry logic and dead letter publishing
     * - Retries failed messages 3 times with 1 second intervals
     * - After all retries are exhausted, sends message to dead letter topic
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Configure retry policy: 3 retries with 1 second delay between attempts
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterPublishingRecoverer(),
                fixedBackOff);

        // Configure which exceptions should trigger retries
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        // Add logging for retry attempts
        errorHandler.setRetryListeners((consumerRecord, exception, deliveryAttempt) -> {
            logger.warn("Retry attempt {} for message from topic: {}, partition: {}, offset: {}, exception: {}",
                    deliveryAttempt,
                    consumerRecord.topic(),
                    consumerRecord.partition(),
                    consumerRecord.offset(),
                    exception.getMessage());
        });

        return errorHandler;
    }

    /**
     * Custom dead letter queue handler for specific business logic
     * This can be used to implement custom recovery strategies
     */
    public static class CustomDeadLetterHandler {

        private static final Logger log = LoggerFactory.getLogger(CustomDeadLetterHandler.class);

        public static void handleDeadLetter(ConsumerRecord<String, Object> record, Exception exception) {
            log.error("Processing dead letter for record: topic={}, partition={}, offset={}, key={}, value={}, exception={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    record.value(),
                    exception.getMessage());

            // TODO: Implement custom dead letter handling logic
            // Examples:
            // - Send alert to monitoring system
            // - Store in database for manual review
            // - Send notification to operations team
            // - Implement custom retry logic with exponential backoff
        }
    }
}