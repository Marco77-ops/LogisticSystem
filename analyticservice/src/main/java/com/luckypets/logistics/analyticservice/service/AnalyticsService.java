package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.shared.events.ShipmentAnalyticsEvent;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;

/**
 * Service for building the analytics stream processing topology.
 */
public interface AnalyticsService {

    /**
     * Build the analytics stream that aggregates delivered shipments per location.
     *
     * @param builder the StreamsBuilder to use for topology construction
     * @return the resulting analytics {@link KStream}
     */
    KStream<String, ShipmentAnalyticsEvent> buildAnalyticsStream(StreamsBuilder builder);
}
