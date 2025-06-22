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
 * Enhanced configuration for error handling in Kafka consumers
 * Implements dead letter queue pattern with comprehensive logging
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

        logger.info("🔧 Configured dead letter producer with bootstrap servers: {}", bootstrapServers);
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
     * Enhanced dead letter publishing recoverer with detailed logging
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate(),
                (consumerRecord, exception) -> {
                    // Enhanced logging for failed message details
                    logger.error("🚨 MESSAGE PROCESSING FAILED!");
                    logger.error("📍 Topic: {}", consumerRecord.topic());
                    logger.error("📍 Partition: {}", consumerRecord.partition());
                    logger.error("📍 Offset: {}", consumerRecord.offset());
                    logger.error("📍 Key: {}", consumerRecord.key());
                    logger.error("📍 Value: {}", consumerRecord.value());
                    logger.error("📍 Headers: {}", consumerRecord.headers());
                    logger.error("❌ Exception: {}", exception.getClass().getSimpleName());
                    logger.error("❌ Message: {}", exception.getMessage());
                    logger.error("❌ Stack trace:", exception);

                    // Custom logic to determine dead letter topic name
                    String deadLetterTopic = consumerRecord.topic() + ".DLT";
                    logger.error("💀 Sending failed message to dead letter topic: {}", deadLetterTopic);

                    // Return TopicPartition with the same partition as the original record
                    return new TopicPartition(deadLetterTopic, consumerRecord.partition());
                });
    }

    /**
     * Enhanced error handler with comprehensive retry logic and logging
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Configure retry policy: 3 retries with 2 second delay between attempts
        FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterPublishingRecoverer(),
                fixedBackOff);

        // Configure which exceptions should trigger retries vs immediate DLQ
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        // Enhanced logging for retry attempts
        errorHandler.setRetryListeners((consumerRecord, exception, deliveryAttempt) -> {
            logger.warn("🔄 RETRY ATTEMPT {} for failed message", deliveryAttempt);
            logger.warn("📍 Topic: {}, Partition: {}, Offset: {}",
                    consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
            logger.warn("❌ Exception: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());

            if (deliveryAttempt >= 3) {
                logger.error("💀 Max retries exceeded - message will be sent to DLT");
            }
        });

        // Add logging when error handler is invoked
        logger.info("🔧 Configured Kafka error handler with 3 retries and 2s delay");

        return errorHandler;
    }

    /**
     * Enhanced custom dead letter queue handler for specific business logic
     */
    public static class CustomDeadLetterHandler {

        private static final Logger log = LoggerFactory.getLogger(CustomDeadLetterHandler.class);

        public static void handleDeadLetter(ConsumerRecord<String, Object> record, Exception exception) {
            log.error("💀 DEAD LETTER PROCESSING");
            log.error("📍 Record: topic={}, partition={}, offset={}, key={}",
                    record.topic(), record.partition(), record.offset(), record.key());
            log.error("📄 Value: {}", record.value());
            log.error("❌ Exception: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());
            log.error("🔧 Headers: {}", record.headers());

            // TODO: Implement custom dead letter handling logic
            // Examples:
            // - Send alert to monitoring system (e.g., Slack, PagerDuty)
            // - Store in database for manual review
            // - Send notification to operations team
            // - Implement custom retry logic with exponential backoff
            // - Create incident ticket in JIRA/ServiceNow

            log.error("⚠️ Dead letter message requires manual intervention");
        }
    }

    /**
     * Debugging method to log current error handler configuration
     */
    public void logErrorHandlerConfig() {
        logger.info("🔧 Error Handler Configuration:");
        logger.info("   - Bootstrap Servers: {}", bootstrapServers);
        logger.info("   - Retry Attempts: 3");
        logger.info("   - Retry Delay: 2000ms");
        logger.info("   - Non-retryable Exceptions: IllegalArgumentException, NullPointerException, ClassCastException");
        logger.info("   - Dead Letter Topic Pattern: [original-topic].DLT");
    }
}