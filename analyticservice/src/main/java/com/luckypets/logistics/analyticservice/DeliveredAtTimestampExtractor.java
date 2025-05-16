package com.luckypets.logistics.analyticservice;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.ZoneOffset;

public class DeliveredAtTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        if (record.value() instanceof ShipmentDeliveredEvent) {
            ShipmentDeliveredEvent event = (ShipmentDeliveredEvent) record.value();
            if (event.getDeliveredAt() != null) {
                return event.getDeliveredAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            }
        }
        return record.timestamp();
    }
}
