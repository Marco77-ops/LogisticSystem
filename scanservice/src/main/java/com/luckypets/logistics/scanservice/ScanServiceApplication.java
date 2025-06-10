package com.luckypets.logistics.scanservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // Import this
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration; // Import this

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
// Removed @EntityScan as it's no longer needed without a database
public class ScanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScanServiceApplication.class, args);
    }
}
