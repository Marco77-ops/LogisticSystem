package com.luckypets.logistics.analyticservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafkaStreams
@SpringBootApplication
public class AnalyticsServiceApplication {

        public static void main(String[] args) {
                SpringApplication.run(AnalyticsServiceApplication.class, args);
        }

}
