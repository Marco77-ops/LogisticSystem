package com.luckypets.logistics.analyticservice.model;

import java.time.LocalDateTime;
import java.util.List;

public class DeliveryAnalytics {
    private final String location;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final List<DeliveryCount> hourlyData;
    private final long totalDeliveries;

    public DeliveryAnalytics(String location, LocalDateTime from, LocalDateTime to,
                             List<DeliveryCount> hourlyData, long totalDeliveries) {
        this.location = location;
        this.from = from;
        this.to = to;
        this.hourlyData = hourlyData;
        this.totalDeliveries = totalDeliveries;
    }

    // Getters
    public String getLocation() { return location; }
    public LocalDateTime getFrom() { return from; }
    public LocalDateTime getTo() { return to; }
    public List<DeliveryCount> getHourlyData() { return hourlyData; }
    public long getTotalDeliveries() { return totalDeliveries; }
}