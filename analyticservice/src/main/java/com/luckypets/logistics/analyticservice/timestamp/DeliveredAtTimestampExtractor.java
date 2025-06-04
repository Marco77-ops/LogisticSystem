package com.luckypets.logistics.analyticservice.timestamp;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.ZoneOffset;

/**
 * Custom timestamp extractor for ShipmentDeliveredEvent.
 * Uses the deliveredAt field as event time for windowing operations.
 */
public class DeliveredAtTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        if (record.value() instanceof ShipmentDeliveredEvent) {
            ShipmentDeliveredEvent event = (ShipmentDeliveredEvent) record.value();
            if (event.getDeliveredAt() != null) {
                return event.getDeliveredAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            }
        }
        // Fallback to record timestamp
        return record.timestamp();
    }
}