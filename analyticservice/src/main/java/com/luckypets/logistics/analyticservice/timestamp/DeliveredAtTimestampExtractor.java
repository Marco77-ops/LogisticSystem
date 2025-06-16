package com.luckypets.logistics.analyticservice.timestamp;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Custom timestamp extractor for ShipmentDeliveredEvent.
 * Uses the deliveredAt field as event time for windowing operations.
 * 
 * This extractor handles timezone conversions and provides robust fallback
 * mechanisms for cases where event timestamps are missing or invalid.
 */
@Component
public class DeliveredAtTimestampExtractor implements TimestampExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DeliveredAtTimestampExtractor.class);

    @Value("${app.default-timezone:Europe/Berlin}")
    private String defaultTimezone;

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        // Validate record and its value
        if (record == null) {
            logger.warn("ConsumerRecord is null, using current time as fallback");
            return System.currentTimeMillis();
        }

        if (record.value() == null) {
            logger.warn("Record value is null, using record timestamp as fallback");
            return getValidTimestamp(record.timestamp());
        }

        // Check if the record contains a ShipmentDeliveredEvent
        if (record.value() instanceof ShipmentDeliveredEvent) {
            ShipmentDeliveredEvent event = (ShipmentDeliveredEvent) record.value();
            
            // Extract timestamp from deliveredAt field
            if (event.getDeliveredAt() != null) {
                try {
                    long extractedTimestamp = event.getDeliveredAt()
                            .atZone(getTimezone())
                            .toInstant()
                            .toEpochMilli();
                    
                    logger.debug("Extracted timestamp {} from deliveredAt field for shipment {}", 
                               extractedTimestamp, event.getShipmentId());
                    return extractedTimestamp;
                    
                } catch (Exception e) {
                    logger.error("Error extracting timestamp from deliveredAt field for shipment {}: {}", 
                               event.getShipmentId(), e.getMessage());
                }
            } else {
                logger.warn("DeliveredAt field is null for shipment {}, using fallback timestamp", 
                           event.getShipmentId());
            }
        } else {
            logger.warn("Record value is not a ShipmentDeliveredEvent (type: {}), using fallback timestamp", 
                       record.value().getClass().getSimpleName());
        }

        // Fallback to record timestamp or current time
        return getValidTimestamp(record.timestamp());
    }

    /**
     * Gets a valid timestamp, falling back to current time if the provided timestamp is invalid.
     */
    private long getValidTimestamp(long timestamp) {
        if (timestamp > 0) {
            logger.debug("Using record timestamp: {}", timestamp);
            return timestamp;
        }
        
        long currentTime = System.currentTimeMillis();
        logger.warn("Record timestamp is invalid ({}), using current time as fallback: {}", 
                   timestamp, currentTime);
        return currentTime;
    }

    /**
     * Gets the timezone to use for timestamp conversion.
     * Defaults to configured timezone or Europe/Berlin if not configured.
     */
    private ZoneId getTimezone() {
        try {
            return ZoneId.of(defaultTimezone);
        } catch (Exception e) {
            logger.warn("Invalid default timezone '{}', falling back to UTC: {}", 
                       defaultTimezone, e.getMessage());
            return ZoneOffset.UTC;
        }
    }

    /**
     * For testing purposes - allows setting the default timezone
     */
    public void setDefaultTimezone(String timezone) {
        this.defaultTimezone = timezone;
    }

    /**
     * For testing purposes - gets the current default timezone
     */
    public String getDefaultTimezone() {
        return this.defaultTimezone;
    }
}