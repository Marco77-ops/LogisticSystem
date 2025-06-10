package com.luckypets.logistics.shipmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // Import this
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration; // Import this

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class ShipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipmentServiceApplication.class, args);
    }
}