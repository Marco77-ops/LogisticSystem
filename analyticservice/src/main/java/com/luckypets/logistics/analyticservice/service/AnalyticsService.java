package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.analyticservice.model.DeliveryAnalytics;
import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    
    private final StateStoreQueryService stateStoreQueryService;
    
    // Constructor Injection statt Field Injection
    public AnalyticsService(StateStoreQueryService stateStoreQueryService) {
        this.stateStoreQueryService = stateStoreQueryService;
    }
    
    public DeliveryAnalytics getLocationAnalytics(String location, LocalDateTime from, LocalDateTime to) {
        // Parametervalidierung
        validateLocationAnalyticsParameters(location, from, to);
        
        logger.info("Getting analytics for location: {} from {} to {}", location, from, to);
        
        try {
            List<DeliveryCount> hourlyCounts = stateStoreQueryService.getDeliveryCountsForLocation(location, from, to);
            
            // Null-Check
            if (hourlyCounts == null) {
                hourlyCounts = new ArrayList<>();
            }
            
            long totalDeliveries = hourlyCounts.stream().mapToLong(DeliveryCount::getCount).sum();
            
            DeliveryAnalytics analytics = new DeliveryAnalytics(location, from, to, hourlyCounts, totalDeliveries);
            
            logger.info("Location analytics for {}: {} hourly records, {} total deliveries",
                    location, hourlyCounts.size(), totalDeliveries);
            
            return analytics;
        } catch (Exception e) {
            logger.error("Error retrieving analytics for location: {}", location, e);
            throw new RuntimeException("Failed to retrieve location analytics", e);
        }
    }
    
    public List<DeliveryCount> getAllLocationAnalytics(LocalDateTime from, LocalDateTime to) {
        // Parametervalidierung
        validateTimeParameters(from, to);
        
        logger.info("Getting analytics for all locations from {} to {}", from, to);
        
        try {
            List<DeliveryCount> allCounts = stateStoreQueryService.getAllDeliveryCounts(from, to);
            
            // Null-Check
            if (allCounts == null) {
                allCounts = new ArrayList<>();
            }
            
            logger.info("Retrieved {} delivery count records across all locations", allCounts.size());
            return allCounts;
        } catch (Exception e) {
            logger.error("Error retrieving analytics for all locations", e);
            throw new RuntimeException("Failed to retrieve all location analytics", e);
        }
    }
    
    private void validateLocationAnalyticsParameters(String location, LocalDateTime from, LocalDateTime to) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        validateTimeParameters(from, to);
    }
    
    private void validateTimeParameters(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            throw new IllegalArgumentException("From date cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("To date cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before or equal to to date");
        }
    }
}