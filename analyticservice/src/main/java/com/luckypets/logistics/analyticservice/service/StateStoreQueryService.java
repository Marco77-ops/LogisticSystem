package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class StateStoreQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StateStoreQueryService.class);

    @Value("${kafka.streams.state-store-name}")
    private String stateStoreName;

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    public List<DeliveryCount> getDeliveryCountsForLocation(String location, LocalDateTime from, LocalDateTime to) {
        logger.debug("Querying delivery counts for location: {} from {} to {}", location, from, to);

        List<DeliveryCount> results = new ArrayList<>();

        try {
            KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();

            if (kafkaStreams == null || !kafkaStreams.state().isRunningOrRebalancing()) {
                logger.warn("Kafka Streams is not running. Current state: {}",
                        kafkaStreams != null ? kafkaStreams.state() : "null");
                return results;
            }

            ReadOnlyWindowStore<String, Long> windowStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(stateStoreName, QueryableStoreTypes.windowStore())
            );

            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);

            try (WindowStoreIterator<Long> iterator = windowStore.fetch(location, fromInstant, toInstant)) {
                while (iterator.hasNext()) {
                    var record = iterator.next();
                    Instant windowStart = Instant.ofEpochMilli(record.key);
                    LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
                    LocalDateTime endTime = startTime.plusHours(1); // Assuming 1-hour windows

                    DeliveryCount count = new DeliveryCount(location, record.value, startTime, endTime);
                    results.add(count);
                    logger.debug("Found delivery count: {}", count);
                }
            }

        } catch (Exception e) {
            logger.error("Error querying state store for location: " + location, e);
        }

        logger.debug("Returning {} delivery count records for location: {}", results.size(), location);
        return results;
    }

    public List<DeliveryCount> getAllDeliveryCounts(LocalDateTime from, LocalDateTime to) {
        logger.debug("Querying all delivery counts from {} to {}", from, to);

        List<DeliveryCount> results = new ArrayList<>();

        try {
            KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();

            if (kafkaStreams == null || !kafkaStreams.state().isRunningOrRebalancing()) {
                logger.warn("Kafka Streams is not running");
                return results;
            }

            ReadOnlyWindowStore<String, Long> windowStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(stateStoreName, QueryableStoreTypes.windowStore())
            );

            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);

            try (var iterator = windowStore.fetchAll(fromInstant, toInstant)) {
                while (iterator.hasNext()) {
                    var record = iterator.next();
                    String location = record.key.key();
                    Instant windowStart = record.key.window().startTime();
                    LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
                    LocalDateTime endTime = startTime.plusHours(1);

                    DeliveryCount count = new DeliveryCount(location, record.value, startTime, endTime);
                    results.add(count);
                }
            }

        } catch (Exception e) {
            logger.error("Error querying all delivery counts", e);
        }

        logger.debug("Returning {} total delivery count records", results.size());
        return results;
    }
}