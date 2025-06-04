package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.shared.events.ShipmentAnalyticsEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import com.luckypets.logistics.analyticservice.DeliveredAtTimestampExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of {@link AnalyticsService} that creates the Kafka Streams topology
 * for aggregating delivered shipments per location.
 */
@Configuration
public class AnalyticsServiceImpl implements AnalyticsService {

    @Value("${kafka.topic.delivered:shipment-delivered}")
    private String deliveredTopic;

    @Value("${kafka.topic.analytics:shipment-analytics}")
    private String analyticsTopic;

    public AnalyticsServiceImpl(
            @Value("${kafka.topic.delivered:shipment-delivered}") String deliveredTopic,
            @Value("${kafka.topic.analytics:shipment-analytics}") String analyticsTopic) {
        this.deliveredTopic = deliveredTopic;
        this.analyticsTopic = analyticsTopic;
    }

    public AnalyticsServiceImpl() {
    }

    @Override
    @Bean
    public KStream<String, ShipmentAnalyticsEvent> buildAnalyticsStream(StreamsBuilder builder) {
        JsonSerde<ShipmentDeliveredEvent> inputSerde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        JsonSerde<ShipmentAnalyticsEvent> outputSerde = new JsonSerde<>(ShipmentAnalyticsEvent.class);

        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1));

        KStream<String, ShipmentDeliveredEvent> inputStream = builder.stream(
                deliveredTopic,
                Consumed.with(Serdes.String(), inputSerde)
                        .withTimestampExtractor(new DeliveredAtTimestampExtractor())
        );

        KTable<Windowed<String>, Long> countsPerLocation = inputStream
                .groupBy((key, event) -> event.getLocation(), Grouped.with(Serdes.String(), inputSerde))
                .windowedBy(windows)
                .count();

        KStream<String, ShipmentAnalyticsEvent> outputStream = countsPerLocation
                .toStream()
                .map((windowedKey, count) -> {
                    ShipmentAnalyticsEvent analyticsEvent = new ShipmentAnalyticsEvent(
                            windowedKey.key(),
                            count,
                            Instant.ofEpochMilli(windowedKey.window().start())
                    );
                    return KeyValue.pair(windowedKey.key(), analyticsEvent);
                })
                .peek((key, event) -> System.out.println("Analytics: " + event));

        outputStream.to(analyticsTopic, Produced.with(Serdes.String(), outputSerde));

        return outputStream;
    }
}
