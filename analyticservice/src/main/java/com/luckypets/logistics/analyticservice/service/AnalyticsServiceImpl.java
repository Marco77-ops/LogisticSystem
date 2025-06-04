package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.shared.events.ShipmentAnalyticsEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
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

    @Value("${kafka.topic.analytics:shipment-analytics}")
    private String analyticsTopic;

    @Override
    @Bean
    public KStream<String, ShipmentAnalyticsEvent> buildAnalyticsStream(StreamsBuilder builder) {
        logger.info("Building analytics stream - Input: {}, Output: {}", deliveredTopic, analyticsTopic);

        // Configure serdes for input and output
        JsonSerde<ShipmentDeliveredEvent> inputSerde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        JsonSerde<ShipmentAnalyticsEvent> outputSerde = new JsonSerde<>(ShipmentAnalyticsEvent.class);

        inputSerde.configure(Map.of("spring.json.trusted.packages", "com.luckypets.logistics.shared.events"), false);
        outputSerde.configure(Map.of("spring.json.trusted.packages", "com.luckypets.logistics.shared.events"), false);

        // Define 1-hour tumbling windows
        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1));

        // Create input stream from delivered events
        KStream<String, ShipmentDeliveredEvent> inputStream = builder.stream(
                deliveredTopic,
                Consumed.with(Serdes.String(), inputSerde)
        );

        // Group by location and count deliveries in time windows
        KTable<Windowed<String>, Long> countsPerLocation = inputStream
                .groupBy((key, event) -> event.getLocation(),
                        Grouped.with(Serdes.String(), inputSerde))
                .windowedBy(windows)
                .count();

        // Transform counts to analytics events
        KStream<String, ShipmentAnalyticsEvent> outputStream = countsPerLocation
                .toStream()
                .map((windowedKey, count) -> {
                    ShipmentAnalyticsEvent analyticsEvent = new ShipmentAnalyticsEvent(
                            windowedKey.key(), // location
                            count,
                            Instant.ofEpochMilli(windowedKey.window().start()) // Use Instant instead of LocalDateTime
                    );
                    return KeyValue.pair(windowedKey.key(), analyticsEvent);
                })
                .peek((key, event) ->
                        logger.info("Analytics aggregation: Location={}, Count={}, Window={}",
                                event.getLocation(), event.getDeliveryCount(), event.getWindowStart()));

        // Send results to analytics topic
        outputStream.to(analyticsTopic, Produced.with(Serdes.String(), outputSerde));

        logger.info("Analytics stream topology built successfully");
        return outputStream;
    }
}