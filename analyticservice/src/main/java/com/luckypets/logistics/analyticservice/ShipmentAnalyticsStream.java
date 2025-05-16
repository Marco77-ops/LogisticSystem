package com.luckypets.logistics.analyticservice;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentAnalyticsEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;

@Configuration
public class ShipmentAnalyticsStream {

    @Bean
    public KStream<String, ShipmentAnalyticsEvent> analyticsStream(StreamsBuilder builder) {
        JsonSerde<ShipmentDeliveredEvent> inputSerde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        JsonSerde<ShipmentAnalyticsEvent> outputSerde = new JsonSerde<>(ShipmentAnalyticsEvent.class);

        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1));

        KStream<String, ShipmentDeliveredEvent> inputStream = builder.stream(
                "shipment-delivered",
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

        outputStream.to("shipment-analytics", Produced.with(Serdes.String(), outputSerde));

        return outputStream;
    }
}
