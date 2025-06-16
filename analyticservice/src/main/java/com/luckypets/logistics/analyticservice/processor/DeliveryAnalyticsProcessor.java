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

    @Value("${kafka.topic.delivery-analytics:delivery-analytics}")
    private String outputTopic;

    @Value("${kafka.streams.state-store-name}")
    private String stateStoreName;

    @Value("${kafka.streams.window.size-minutes}")
    private int windowSizeMinutes;

    @Value("${kafka.streams.window.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Value("${kafka.streams.state-store.retention-days:7}")
    private int stateStoreRetentionDays;

    @Autowired
    private JsonSerde<ShipmentDeliveredEvent> shipmentDeliveredEventSerde;

    @Autowired
    private JsonSerde<DeliveryCount> deliveryCountSerde;

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        logger.info("Building Kafka Streams pipeline for delivery analytics");
        logger.info("Input topic: {}, Output topic: {}, State store: {}, Window size: {} minutes, Grace period: {} minutes",
                inputTopic, outputTopic, stateStoreName, windowSizeMinutes, gracePeriodMinutes);

        try {
            // Define time windows with grace period
            TimeWindows timeWindows = TimeWindows
                    .ofSizeAndGrace(Duration.ofMinutes(windowSizeMinutes), Duration.ofMinutes(gracePeriodMinutes))
                    .advanceBy(Duration.ofMinutes(windowSizeMinutes));

            // Build the topology
            KStream<String, ShipmentDeliveredEvent> deliveryStream = streamsBuilder
                    .stream(inputTopic, Consumed.with(Serdes.String(), shipmentDeliveredEventSerde));

            deliveryStream
                    // Filter out null events and events with null/empty locations
                    .filter((key, event) -> {
                        if (event == null) {
                            logger.warn("Received null event for key: {}", key);
                            return false;
                        }
                        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
                            logger.warn("Received event with null/empty location for shipment: {}", event.getShipmentId());
                            return false;
                        }
                        return true;
                    })
                    // Only log at debug level for high volume scenarios
                    .peek((key, event) -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Processing delivery event: {} at location: {}",
                                    event.getShipmentId(), event.getLocation());
                        }
                    })
                    // Group by location
                    .groupBy((key, event) -> event.getLocation().trim(),
                            Grouped.with(Serdes.String(), shipmentDeliveredEventSerde))
                    // Window by time
                    .windowedBy(timeWindows)
                    // Count deliveries per location per window
                    .count(Materialized.<String, Long>as(
                                    Stores.persistentTimestampedWindowStore(
                                            stateStoreName, 
                                            Duration.ofDays(stateStoreRetentionDays), 
                                            Duration.ofMinutes(windowSizeMinutes), 
                                            false))
                            .withKeySerde(Serdes.String())
                            .withValueSerde(Serdes.Long()))
                    // Convert to DeliveryCount objects
                    .toStream()
                    .map((windowedKey, count) -> {
                        try {
                            String location = windowedKey.key();
                            Instant windowStart = windowedKey.window().startTime();
                            Instant windowEnd = windowedKey.window().endTime();

                            LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
                            LocalDateTime endTime = LocalDateTime.ofInstant(windowEnd, ZoneOffset.UTC);

                            DeliveryCount deliveryCount = new DeliveryCount(location, count, startTime, endTime);

                            if (logger.isDebugEnabled()) {
                                logger.debug("Aggregated delivery count: {}", deliveryCount);
                            }
                            
                            return KeyValue.pair(location + "-" + windowStart.toEpochMilli(), deliveryCount);
                        } catch (Exception e) {
                            logger.error("Error processing windowed aggregation for key: {}, count: {}", windowedKey, count, e);
                            // Return null to filter out problematic records
                            return null;
                        }
                    })
                    // Filter out null results from error handling
                    .filter((key, value) -> value != null)
                    // Reduced logging frequency for production
                    .peek((key, value) -> {
                        if (logger.isInfoEnabled()) {
                            logger.info("Publishing aggregated result: {} deliveries at {}", 
                                    value.getCount(), value.getLocation());
                        }
                    })
                    // Send results to output topic
                    .to(outputTopic, Produced.with(Serdes.String(), deliveryCountSerde));

            logger.info("Kafka Streams pipeline built successfully");

        } catch (Exception e) {
            logger.error("Failed to build Kafka Streams pipeline", e);
            throw new RuntimeException("Pipeline configuration failed", e);
        }
    }
}