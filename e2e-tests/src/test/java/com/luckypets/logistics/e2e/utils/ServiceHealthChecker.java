package com.luckypets.logistics.e2e.utils;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

import java.time.Duration;

import static com.luckypets.logistics.e2e.config.TestConstants.*;
import static io.restassured.RestAssured.given;

@Slf4j
public class ServiceHealthChecker {

    public static void waitForAllServices() {
        log.info("🔍 Warte auf alle Services...");

        waitForService("ShipmentService", SHIPMENT_PORT, "/actuator/health");
        waitForService("ScanService", SCAN_PORT, "/actuator/health");
        waitForService("NotificationService", NOTIFICATION_PORT, "/actuator/health");

        // DeliveryService und AnalyticsService haben möglicherweise keine Health-Endpoints
        waitForServiceWithFallback("DeliveryService", DELIVERY_PORT, "/actuator/health", "/deliveries/probe");
        waitForServiceWithFallback("AnalyticsService", ANALYTICS_PORT, "/actuator/health", "/api/analytics/deliveries");

        log.info("✅ Alle Services sind bereit!");
    }

    private static void waitForService(String serviceName, int port, String healthEndpoint) {
        Awaitility.await(serviceName + " Health Check")
                .atMost(HEALTH_CHECK_TIMEOUT)
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    try {
                        given()
                                .when().get(BASE_URL + ":" + port + healthEndpoint)
                                .then().statusCode(200);
                        log.info("✅ {} is healthy", serviceName);
                    } catch (Exception e) {
                        log.warn("⏳ Waiting for {}... ({})", serviceName, e.getMessage());
                        throw e;
                    }
                });
    }

    private static void waitForServiceWithFallback(String serviceName, int port, String healthEndpoint, String fallbackEndpoint) {
        Awaitility.await(serviceName + " Health Check")
                .atMost(HEALTH_CHECK_TIMEOUT)
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    try {
                        // Versuche zuerst Health-Endpoint
                        given()
                                .when().get(BASE_URL + ":" + port + healthEndpoint)
                                .then().statusCode(200);
                        log.info("✅ {} is healthy (health endpoint)", serviceName);
                    } catch (Exception e) {
                        // Fallback auf Service-Endpoint
                        try {
                            int status = given()
                                    .when().get(BASE_URL + ":" + port + fallbackEndpoint)
                                    .getStatusCode();
                            if (status < 500) { // 200, 404, 400 sind OK - Service läuft
                                log.info("✅ {} is running (fallback check, status: {})", serviceName, status);
                            } else {
                                throw new RuntimeException("Service nicht erreichbar, Status: " + status);
                            }
                        } catch (Exception fallbackEx) {
                            log.warn("⏳ Waiting for {}... (health: {}, fallback: {})",
                                    serviceName, e.getMessage(), fallbackEx.getMessage());
                            throw fallbackEx;
                        }
                    }
                });
    }

    public static boolean isServiceHealthy(String serviceName, int port, String endpoint) {
        try {
            Response response = given().when().get(BASE_URL + ":" + port + endpoint);
            return response.getStatusCode() == 200;
        } catch (Exception e) {
            log.debug("Service {} nicht erreichbar: {}", serviceName, e.getMessage());
            return false;
        }
    }
}