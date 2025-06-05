package com.luckypets.logistics.analyticservice.service;

import org.apache.kafka.streams.StreamsBuilder;

/**
 * Service for building analytics stream processing topology.
 * Aggregates delivery events per location and time window.
 */
public interface AnalyticsService {

    /**
     * Builds the analytics stream that processes delivered shipments.
     * Creates hourly aggregations of deliveries per location.
     *
     * @param builder the StreamsBuilder to use for topology construction
 */
    void buildAnalyticsTopology(StreamsBuilder builder);
}