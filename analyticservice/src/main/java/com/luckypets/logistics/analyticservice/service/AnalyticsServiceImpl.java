package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Implementation of AnalyticsService that creates Kafka Streams topology
 * for aggregating delivered shipments per location and time window.
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    @Value("${kafka.topic.delivered:shipment-delivered}")
    private String deliveredTopic;

    private static final String STORE_NAME = "deliveries-per-location";

    @Override
    @Bean
    public void buildAnalyticsTopology(StreamsBuilder builder) {
        logger.info("Building analytics topology - Input topic: {}", deliveredTopic);

        // Configure serde for input events
        JsonSerde<ShipmentDeliveredEvent> inputSerde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        inputSerde.configure(Map.of("spring.json.trusted.packages", "com.luckypets.logistics.shared.events"), false);

        // Define 1-hour tumbling windows
        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1));

        // Create input stream from delivered events
        KStream<String, ShipmentDeliveredEvent> inputStream = builder.stream(
                deliveredTopic,
                Consumed.with(Serdes.String(), inputSerde)
        );

        // Group by location and count deliveries in time windows. Results are materialized
        // in a named store to allow querying via Kafka Streams API.
        KTable<Windowed<String>, Long> countsPerLocation = inputStream
                .groupBy((key, event) -> event.getLocation(),
                        Grouped.with(Serdes.String(), inputSerde))
                .windowedBy(windows)
                .count(Materialized.as(STORE_NAME));

        // Log the aggregations for observability
        countsPerLocation.toStream()
                .foreach((windowedKey, count) -> logger.info(
                        "Analytics aggregation: Location={}, Count={}, Window={}",
                        windowedKey.key(), count,
                        Instant.ofEpochMilli(windowedKey.window().start())));

        logger.info("Analytics topology built successfully");
    }
}