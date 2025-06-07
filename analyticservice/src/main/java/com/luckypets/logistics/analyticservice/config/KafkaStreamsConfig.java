package com.luckypets.logistics.analyticservice.config;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public Serde<ShipmentDeliveredEvent> shipmentDeliveredEventSerde() {
        JsonSerde<ShipmentDeliveredEvent> serde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        serde.configure(Map.of("spring.json.trusted.packages", "com.luckypets.logistics.shared.events"), false);
        return serde;
    }
}