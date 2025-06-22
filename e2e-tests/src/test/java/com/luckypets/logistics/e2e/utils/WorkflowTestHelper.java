package com.luckypets.logistics.e2e.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

import static com.luckypets.logistics.e2e.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class WorkflowTestHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String createShipment(String origin, String destination, String customerId) {
        log.info("📦 Erstelle Sendung: {} → {} (Customer: {})", origin, destination, customerId);

        Response response = given()
                .contentType("application/json")
                .body(String.format("""
                {
                    "origin": "%s",
                    "destination": "%s",
                    "customerId": "%s"
                }
                """, origin, destination, customerId))
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(201)
                .body("id", notNullValue())
                .extract().response();

        String shipmentId = response.jsonPath().getString("id");
        log.info("✅ Sendung erstellt: {}", shipmentId);
        return shipmentId;
    }

    public static void scanShipment(String shipmentId, String location) {
        log.info("📍 Scanne Sendung {} an Location: {}", shipmentId, location);

        given()
                .contentType("application/json")
                .body(String.format("""
                {
                    "shipmentId": "%s",
                    "location": "%s"
                }
                """, shipmentId, location))
                .when().post(BASE_URL + ":" + SCAN_PORT + "/api/v1/scans")
                .then().statusCode(201);

        log.info("✅ Sendung gescannt");
    }

    public static void waitForDelivery(String shipmentId) {
        log.info("⏳ Warte auf Zustellung von Sendung: {}", shipmentId);

        Awaitility.await("Delivery of " + shipmentId)
                .atMost(EVENT_PROCESSING_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    try {
                        Response response = given()
                                .when().get(BASE_URL + ":" + DELIVERY_PORT + "/deliveries/" + shipmentId);

                        assertEquals(200, response.getStatusCode(),
                                "DeliveryService sollte erreichbar sein");
                        assertEquals("DELIVERED", response.jsonPath().getString("status"),
                                "Sendung sollte zugestellt sein");

                        log.info("✅ Zustellung bestätigt");
                    } catch (AssertionError e) {
                        log.debug("⏳ Warte weiter auf Zustellung...");
                        throw e;
                    }
                });
    }

    public static void waitForNotifications(String shipmentId, int minCount) {
        log.info("⏳ Warte auf Benachrichtigungen für Sendung: {}", shipmentId);

        Awaitility.await("Notifications for " + shipmentId)
                .atMost(NOTIFICATION_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    try {
                        Response response = given()
                                .when().get(BASE_URL + ":" + NOTIFICATION_PORT + "/api/notifications")
                                .then().statusCode(200)
                                .extract().response();

                        JsonNode notifications = objectMapper.readTree(response.getBody().asString());
                        assertTrue(notifications.isArray(), "Benachrichtigungen sollten Array sein");

                        long count = 0;
                        for (JsonNode notification : notifications) {
                            if (notification.has("shipmentId") &&
                                    shipmentId.equals(notification.get("shipmentId").asText())) {
                                count++;
                            }
                        }

                        assertTrue(count >= minCount,
                                String.format("Erwartet >= %d Benachrichtigungen, aber nur %d gefunden",
                                        minCount, count));

                        log.info("✅ {} Benachrichtigungen gefunden", count);
                    } catch (Exception e) {
                        log.debug("⏳ Warte weiter auf Benachrichtigungen...");
                        throw e;
                    }
                });
    }

    public static void checkAnalytics(int minEntries) {
        if (!ServiceHealthChecker.isServiceHealthy("AnalyticsService", ANALYTICS_PORT, "/api/analytics/deliveries")) {
            log.warn("⚠️ AnalyticsService nicht verfügbar - überspringe Analytics-Check");
            return;
        }

        log.info("⏳ Warte auf Analytics-Daten...");

        Awaitility.await("Analytics Data")
                .atMost(ANALYTICS_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    try {
                        Response response = given()
                                .when().get(BASE_URL + ":" + ANALYTICS_PORT + "/api/analytics/deliveries")
                                .then().statusCode(200)
                                .extract().response();

                        JsonNode analytics = objectMapper.readTree(response.getBody().asString());
                        assertTrue(analytics.isArray(), "Analytics sollten Array sein");

                        if (analytics.size() >= minEntries) {
                            log.info("✅ Analytics verfügbar: {} Einträge", analytics.size());
                        } else {
                            log.debug("⏳ Nur {} Analytics-Einträge, warte auf {}", analytics.size(), minEntries);
                            fail("Nicht genug Analytics-Einträge");
                        }
                    } catch (Exception e) {
                        log.debug("⏳ Warte weiter auf Analytics...");
                        throw e;
                    }
                });
    }
}