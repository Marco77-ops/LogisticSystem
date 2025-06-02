package com.luckypets.logistics.scanservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"com.luckypets.logistics.shared.model"})
public class ScanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScanServiceApplication.class, args);
    }
}
