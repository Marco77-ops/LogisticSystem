package com.luckypets.logistics.analyticservice.processor;

import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class DeliveryAnalyticsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryAnalyticsProcessor.class);

    @Value("${kafka.topic.delivered}")
    private String inputTopic;

    @Value("${kafka.streams.state-store-name}")
    private String stateStoreName;

    @Value("${kafka.streams.window.size-minutes}")
    private int windowSizeMinutes;

    @Value("${kafka.streams.window.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Autowired
    private JsonSerde<ShipmentDeliveredEvent> shipmentDeliveredEventSerde;

    @Autowired
    private JsonSerde<DeliveryCount> deliveryCountSerde;

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        logger.info("Building Kafka Streams pipeline for delivery analytics");
        logger.info("Input topic: {}, State store: {}, Window size: {} minutes",
                inputTopic, stateStoreName, windowSizeMinutes);

        // Define time windows
        TimeWindows timeWindows = TimeWindows
                .ofSizeWithNoGrace(Duration.ofMinutes(windowSizeMinutes))
                .advanceBy(Duration.ofMinutes(windowSizeMinutes));

        // Build the topology
        KStream<String, ShipmentDeliveredEvent> deliveryStream = streamsBuilder
                .stream(inputTopic, Consumed.with(Serdes.String(), shipmentDeliveredEventSerde));

        deliveryStream
                .peek((key, event) -> logger.debug("Processing delivery event: {} at location: {}",
                        event.getShipmentId(), event.getLocation()))
                // Group by location
                .groupBy((key, event) -> event.getLocation(),
                        Grouped.with(Serdes.String(), shipmentDeliveredEventSerde))
                // Window by time
                .windowedBy(timeWindows)
                // Count deliveries per location per window
                .count(Materialized.<String, Long>as(
                                Stores.persistentTimestampedWindowStore(stateStoreName, Duration.ofDays(7), Duration.ofMinutes(windowSizeMinutes), false))
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                // Convert to DeliveryCount objects
                .toStream()
                .map((windowedKey, count) -> {
                    String location = windowedKey.key();
                    Instant windowStart = windowedKey.window().startTime();
                    Instant windowEnd = windowedKey.window().endTime();

                    LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
                    LocalDateTime endTime = LocalDateTime.ofInstant(windowEnd, ZoneOffset.UTC);

                    DeliveryCount deliveryCount = new DeliveryCount(location, count, startTime, endTime);

                    logger.debug("Aggregated delivery count: {}", deliveryCount);
                    return KeyValue.pair(location + "-" + windowStart.toEpochMilli(), deliveryCount);
                })
                // Log for debugging
                .peek((key, value) -> logger.info("Final aggregated result: {} -> {}", key, value));

        logger.info("Kafka Streams pipeline built successfully");
    }
}