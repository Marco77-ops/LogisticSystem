package com.luckypets.logistics.notificationviewservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "com.luckypets.logistics.notificationviewservice",
        "com.luckypets.logistics.shared.config"
})
public class NotificationViewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationViewServiceApplication.class, args);
    }
}