package com.luckypets.logistics.analyticservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableKafkaStreams
@ComponentScan(basePackages = {
        "com.luckypets.logistics.analyticservice",
        "com.luckypets.logistics.shared.config"
})
public class AnalyticServiceApplication {
        public static void main(String[] args) {
                SpringApplication.run(AnalyticServiceApplication.class, args);
        }
}