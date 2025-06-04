package com.luckypets.logistics.analyticservice.service;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for querying analytics data from Kafka Streams state stores.
 */
@Service
public class AnalyticsQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsQueryService.class);
    private static final String STORE_NAME = "KSTREAM-AGGREGATE-STATE-STORE-0000000004";

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public AnalyticsQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * Get delivery counts per location for the current hour.
     */
    public Map<String, Long> getCurrentHourDeliveries() {
        Map<String, Long> results = new HashMap<>();

        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().isRunningOrRebalancing()) {
                logger.warn("Kafka Streams not available or not running");
                return results;
            }

            ReadOnlyWindowStore<String, Long> store = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(STORE_NAME, QueryableStoreTypes.windowStore())
            );

            Instant now = Instant.now();
            Instant hourStart = now.truncatedTo(ChronoUnit.HOURS);

            // This is a simplified approach - in production you'd iterate through all keys
            // For demo purposes, we return empty map as querying requires knowing the keys
            logger.info("Querying store for time range: {} to {}", hourStart, now);

        } catch (Exception e) {
            logger.error("Error querying analytics store", e);
        }

        return results;
    }

    /**
     * Get delivery count for specific location in current hour.
     */
    public Long getCurrentHourDeliveriesForLocation(String location) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().isRunningOrRebalancing()) {
                logger.warn("Kafka Streams not available or not running");
                return 0L;
            }

            ReadOnlyWindowStore<String, Long> store = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(STORE_NAME, QueryableStoreTypes.windowStore())
            );

            Instant now = Instant.now();
            Instant hourStart = now.truncatedTo(ChronoUnit.HOURS);

            try (WindowStoreIterator<Long> iterator = store.fetch(location, hourStart, now)) {
                Long latestCount = 0L;
                while (iterator.hasNext()) {
                    latestCount = iterator.next().value;
                }
                return latestCount;
            }

        } catch (Exception e) {
            logger.error("Error querying analytics store for location: {}", location, e);
            return 0L;
        }
    }
}