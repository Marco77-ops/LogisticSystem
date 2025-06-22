package com.luckypets.logistics.e2e.config;

import java.time.Duration;

public final class TestConstants {
    private TestConstants() {} // Utility class

    // Base URLs
    public static final String BASE_URL = "http://localhost";

    // Service Ports
    public static final int SHIPMENT_PORT = 8081;
    public static final int SCAN_PORT = 8082;
    public static final int DELIVERY_PORT = 8083;
    public static final int ANALYTICS_PORT = 8084;
    public static final int NOTIFICATION_PORT = 8085;

    // Timeouts - standardisierte Werte
    public static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration EVENT_PROCESSING_TIMEOUT = Duration.ofSeconds(20);
    public static final Duration ANALYTICS_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration BULK_PROCESSING_TIMEOUT = Duration.ofSeconds(30);

    // Polling intervals
    public static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    public static final Duration FAST_POLL_INTERVAL = Duration.ofMillis(500);

    // Test data
    public static final String DEFAULT_CUSTOMER_PREFIX = "e2e-customer";
    public static final String DEFAULT_ORIGIN = "Berlin";
    public static final String DEFAULT_DESTINATION = "Munich";
}