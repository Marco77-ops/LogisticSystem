package com.luckypets.logistics.analyticservice;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class DeliveredAtTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        if (record.value() instanceof ShipmentDeliveredEvent event && event.getTimestamp() != null) {
            return event.getTimestamp().toEpochMilli();
        }
        return record.timestamp();
    }
}