package com.luckypets.logistics.notificationviewservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // Remove custom consumer factory and listener container factory
    // Let Spring Boot auto-configuration handle it based on application.yml properties
}
