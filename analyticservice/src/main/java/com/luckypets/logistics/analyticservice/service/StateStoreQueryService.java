package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
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
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.kstream.Windowed;

@Service
public class StateStoreQueryService {
    private static final Logger logger = LoggerFactory.getLogger(StateStoreQueryService.class);
    
    @Value("${kafka.streams.state-store-name}")
    private String stateStoreName;
    
    @Value("${kafka.streams.window-size-hours:1}")
    private int windowSizeHours;
    
    @Value("${kafka.streams.max-results:1000}")
    private int maxResults;
    
    @Autowired
    private StreamsBuilderFactoryBean factoryBean;
    
    public List<DeliveryCount> getDeliveryCountsForLocation(String location, LocalDateTime from, LocalDateTime to) {
        // Parameter validation
        validateParameters(location, from, to);
        
        logger.debug("Querying delivery counts for location: {} from {} to {}", location, from, to);
        List<DeliveryCount> results = new ArrayList<>();
        
        try {
            ReadOnlyWindowStore<String, Long> windowStore = getWindowStore();
            
            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);
            
            try (WindowStoreIterator<Long> iterator = windowStore.fetch(location, fromInstant, toInstant)) {
                while (iterator.hasNext() && results.size() < maxResults) {
                    var record = iterator.next();
                    DeliveryCount count = createDeliveryCount(location, record);
                    results.add(count);
                    logger.debug("Found delivery count: {}", count);
                }
            }
        } catch (InvalidStateStoreException e) {
            logger.warn("State store not available for location query: {}", e.getMessage());
            return results;
        } catch (Exception e) {
            logger.error("Unexpected error querying state store for location: {}", location, e);
            throw new RuntimeException("Failed to query delivery counts", e);
        }
        
        logger.debug("Returning {} delivery count records for location: {}", results.size(), location);
        return results;
    }
    
    // Neue Methode hinzufügen
    public List<DeliveryCount> getAllDeliveryCounts(LocalDateTime from, LocalDateTime to) {
        validateTimeParameters(from, to);
        
        logger.debug("Querying delivery counts for all locations from {} to {}", from, to);
        List<DeliveryCount> results = new ArrayList<>();
        
        try {
            ReadOnlyWindowStore<String, Long> windowStore = getWindowStore();
            
            Instant fromInstant = from.toInstant(ZoneOffset.UTC);
            Instant toInstant = to.toInstant(ZoneOffset.UTC);
            
            try (KeyValueIterator<Windowed<String>, Long> iterator = windowStore.fetchAll(fromInstant, toInstant)) {
                while (iterator.hasNext() && results.size() < maxResults) {
                    var record = iterator.next();
                    String location = record.key.key();
                    long windowStartMs = record.key.window().start();
                    
                    Instant windowStart = Instant.ofEpochMilli(windowStartMs);
                    LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
                    LocalDateTime endTime = startTime.plusHours(windowSizeHours);
                    
                    DeliveryCount count = new DeliveryCount(location, record.value, startTime, endTime);
                    results.add(count);
                    logger.debug("Found delivery count: {}", count);
                }
            }
        } catch (InvalidStateStoreException e) {
            logger.warn("State store not available for all locations query: {}", e.getMessage());
            return results;
        } catch (Exception e) {
            logger.error("Unexpected error querying state store for all locations", e);
            throw new RuntimeException("Failed to query all delivery counts", e);
        }
        
        logger.debug("Returning {} delivery count records for all locations", results.size());
        return results;
    }
    
    private ReadOnlyWindowStore<String, Long> getWindowStore() {
        KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
        
        if (kafkaStreams == null) {
            throw new IllegalStateException("Kafka Streams instance is null");
        }
        
        if (!kafkaStreams.state().isRunningOrRebalancing()) {
            throw new InvalidStateStoreException(
                String.format("Kafka Streams is not running. Current state: %s", kafkaStreams.state())
            );
        }
        
        return kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(stateStoreName, QueryableStoreTypes.windowStore())
        );
    }
    
    private void validateParameters(String location, LocalDateTime from, LocalDateTime to) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        validateTimeParameters(from, to);
    }
    
    private void validateTimeParameters(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            throw new IllegalArgumentException("From date cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("To date cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before or equal to to date");
        }
    }
    
    private DeliveryCount createDeliveryCount(String location, KeyValue<Long, Long> record) {
        Instant windowStart = Instant.ofEpochMilli(record.key);
        LocalDateTime startTime = LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC);
        LocalDateTime endTime = startTime.plusHours(windowSizeHours);
        return new DeliveryCount(location, record.value, startTime, endTime);
    }
}