package com.luckypets.logistics.analyticservice.controller;

import com.luckypets.logistics.analyticservice.model.DeliveryAnalytics;
import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import com.luckypets.logistics.analyticservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<DeliveryAnalytics> getLocationAnalytics(
            @RequestParam String location,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        logger.info("REST request for location analytics: location={}, from={}, to={}", location, from, to);

        try {
            DeliveryAnalytics analytics = analyticsService.getLocationAnalytics(location, from, to);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Error processing analytics request for location: " + location, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<DeliveryCount>> getAllAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        logger.info("REST request for all location analytics: from={}, to={}", from, to);

        try {
            List<DeliveryCount> analytics = analyticsService.getAllLocationAnalytics(from, to);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Error processing analytics request for all locations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics Service is running");
    }
}