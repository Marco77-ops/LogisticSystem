package com.luckypets.logistics.analyticservice.service;

import com.luckypets.logistics.analyticservice.model.DeliveryAnalytics;
import com.luckypets.logistics.analyticservice.model.DeliveryCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private StateStoreQueryService stateStoreQueryService;

    public DeliveryAnalytics getLocationAnalytics(String location, LocalDateTime from, LocalDateTime to) {
        logger.info("Getting analytics for location: {} from {} to {}", location, from, to);

        List<DeliveryCount> hourlyCounts = stateStoreQueryService.getDeliveryCountsForLocation(location, from, to);
        long totalDeliveries = hourlyCounts.stream().mapToLong(DeliveryCount::getCount).sum();

        DeliveryAnalytics analytics = new DeliveryAnalytics(location, from, to, hourlyCounts, totalDeliveries);

        logger.info("Location analytics for {}: {} hourly records, {} total deliveries",
                location, hourlyCounts.size(), totalDeliveries);

        return analytics;
    }

    public List<DeliveryCount> getAllLocationAnalytics(LocalDateTime from, LocalDateTime to) {
        logger.info("Getting analytics for all locations from {} to {}", from, to);

        List<DeliveryCount> allCounts = stateStoreQueryService.getAllDeliveryCounts(from, to);

        logger.info("Retrieved {} delivery count records across all locations", allCounts.size());

        return allCounts;
    }
}