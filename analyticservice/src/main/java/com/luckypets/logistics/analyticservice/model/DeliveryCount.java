package com.luckypets.logistics.analyticservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeliveryCount {
    private final String location;
    private final long count;
    private final LocalDateTime windowStart;
    private final LocalDateTime windowEnd;

    @JsonCreator
    public DeliveryCount(
            @JsonProperty("location") String location,
            @JsonProperty("count") long count,
            @JsonProperty("windowStart") LocalDateTime windowStart,
            @JsonProperty("windowEnd") LocalDateTime windowEnd
    ) {
        this.location = location;
        this.count = count;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    public DeliveryCount(String location, long count) {
        this(location, count, null, null);
    }

    public String getLocation() {
        return location;
    }

    public long getCount() {
        return count;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public DeliveryCount add(long additional) {
        return new DeliveryCount(location, count + additional, windowStart, windowEnd);
    }

    @Override
    public String toString() {
        double hoursInWindow = calculateHoursInWindow();
        double deliveriesPerHour = hoursInWindow > 0 ? count / hoursInWindow : 0;

        return String.format(
                "📊 DELIVERY METRICS ─────────────────────%n" +
                        "🏪 Location    │ %s%n" +
                        "📦 Total       │ %,d deliveries%n" +
                        "⚡ Rate        │ %.1f deliveries/hour%n" +
                        "🕐 Timeframe   │ %s → %s%n" +
                        "───────────────────────────────────────",
                location, count, deliveriesPerHour,
                formatTimestamp(windowStart),
                formatTimestamp(windowEnd)
        );
    }

    /**
     * Helper method for better timestamp formatting
     */
    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "N/A";
        return timestamp.format(DateTimeFormatter.ofPattern("MMM dd HH:mm"));
    }

    /**
     * Helper method to calculate hours in window
     */
    private double calculateHoursInWindow() {
        if (windowStart == null || windowEnd == null) return 0;
        return Duration.between(windowStart, windowEnd).toMinutes() / 60.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeliveryCount that = (DeliveryCount) o;

        if (count != that.count) return false;
        if (!location.equals(that.location)) return false;
        if (windowStart != null ? !windowStart.equals(that.windowStart) : that.windowStart != null) return false;
        return windowEnd != null ? windowEnd.equals(that.windowEnd) : that.windowEnd == null;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + (int) (count ^ (count >>> 32));
        result = 31 * result + (windowStart != null ? windowStart.hashCode() : 0);
        result = 31 * result + (windowEnd != null ? windowEnd.hashCode() : 0);
        return result;
    }
}