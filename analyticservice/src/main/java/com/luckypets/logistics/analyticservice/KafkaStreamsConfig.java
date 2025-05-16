package com.luckypets.logistics.analyticservice;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = "defaultKafkaStreamsConfig")
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "analytics-service");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put("spring.json.trusted.packages", "com.luckypets.logistics.shared.events");
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
                com.luckypets.logistics.analyticservice.DeliveredAtTimestampExtractor.class);



        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<ShipmentDeliveredEvent> shipmentDeliveredEventSerde() {
        JsonSerde<ShipmentDeliveredEvent> serde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        serde.configure(Map.of("spring.json.trusted.packages", "com.luckypets.logistics.shared.events"), false);
        return serde;
    }
}
