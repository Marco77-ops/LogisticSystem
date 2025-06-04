package com.luckypets.logistics.analyticservice.controller;

import com.luckypets.logistics.analyticservice.service.AnalyticsQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for querying analytics data.
 * Provides endpoints to retrieve aggregated delivery statistics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Get delivery counts per location for current hour.
     */
    @GetMapping("/deliveries/current-hour")
    public ResponseEntity<Map<String, Long>> getCurrentHourDeliveries() {
        logger.info("Querying current hour delivery statistics");
        Map<String, Long> stats = queryService.getCurrentHourDeliveries();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get delivery count for specific location in current hour.
     */
    @GetMapping("/deliveries/current-hour/{location}")
    public ResponseEntity<Long> getCurrentHourDeliveriesForLocation(@PathVariable String location) {
        logger.info("Querying current hour deliveries for location: {}", location);
        Long count = queryService.getCurrentHourDeliveriesForLocation(location);
        return ResponseEntity.ok(count != null ? count : 0L);
    }
}