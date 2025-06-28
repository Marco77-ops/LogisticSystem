package com.luckypets.logistics.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic shipmentCreatedTopic() {
        return TopicBuilder.name("shipment-created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic shipmentScannedTopic() {
        return TopicBuilder.name("shipment-scanned")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic shipmentDeliveredTopic() {
        return TopicBuilder.name("shipment-delivered")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deliveryAnalyticsTopic() {
        return TopicBuilder.name("delivery-analytics")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic shipmentAnalyticsTopic() {
        return TopicBuilder.name("shipment-analytics")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ← DAS FEHLTE!
    @Bean
    public NewTopic notificationSentTopic() {
        return TopicBuilder.name("notification-sent")
                .partitions(3)
                .replicas(1)
                .build();
    }
}