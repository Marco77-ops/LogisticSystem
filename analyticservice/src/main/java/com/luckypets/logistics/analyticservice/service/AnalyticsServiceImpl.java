package com.luckypets.logistics.analyticservice.service;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private static final String STORE_NAME = "deliveries-per-location";

    @Bean
    public Topology buildAnalyticsTopology(
            @Value("${kafka.topic.delivered:shipment-delivered}") String deliveredTopic
    ) {
        System.err.println("DEBUG >>> deliveredTopic = [" + deliveredTopic + "]");
        logger.info("Building analytics topology - Input topic: '{}'", deliveredTopic);

        StreamsBuilder builder = new StreamsBuilder();

        // Nur ein trivialer Stream, keine weiteren Operatoren!
        KStream<String, String> testStream = builder.stream(
                deliveredTopic,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        testStream.foreach((key, value) ->
                logger.info("TEST-STREAM received message: key={}, value={}", key, value)
        );

        logger.info("Minimal analytics topology built successfully");
        return builder.build();
    }
}
