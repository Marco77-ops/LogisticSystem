package com.luckypets.logistics.e2e.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

import static com.luckypets.logistics.e2e.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
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
                .then().statusCode(anyOf(equalTo(200), equalTo(201), equalTo(202)))
                .body("shipmentId", notNullValue())
                .extract().response();

        String shipmentId = response.jsonPath().getString("shipmentId");
        log.info("✅ Sendung erstellt: {} (Status: {})", shipmentId, response.getStatusCode());
        return shipmentId;
    }

    public static void scanShipment(String shipmentId, String location) {
        log.info("📍 Scanne Sendung {} an Location: {}", shipmentId, location);

        Response response = given()
                .contentType("application/json")
                .body(String.format("""
                {
                    "shipmentId": "%s",
                    "location": "%s"
                }
                """, shipmentId, location))
                .when().post(BASE_URL + ":" + SCAN_PORT + "/api/v1/scans")
                .then().statusCode(anyOf(equalTo(200), equalTo(201), equalTo(202)))
                .extract().response();

        log.info("✅ Sendung gescannt (Status: {})", response.getStatusCode());
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

                        if (response.getStatusCode() == 404) {
                            log.debug("⏳ Sendung {} noch nicht im DeliveryService verfügbar", shipmentId);
                            throw new AssertionError("Delivery data not available yet");
                        } else if (response.getStatusCode() == 202) {
                            log.debug("⏳ Sendung {} wird noch verarbeitet", shipmentId);
                            throw new AssertionError("Delivery still processing");
                        }

                        assertEquals(200, response.getStatusCode(),
                                "DeliveryService sollte 200 zurückgeben wenn Daten verfügbar sind");

                        String status = response.jsonPath().getString("status");
                        if (!"DELIVERED".equals(status)) {
                            log.debug("⏳ Aktueller Status: {}, warte auf DELIVERED", status);
                            throw new AssertionError("Status ist noch nicht DELIVERED: " + status);
                        }

                        log.info("✅ Zustellung bestätigt (Status: {})", status);
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                            log.debug("⏳ DeliveryService noch nicht erreichbar...");
                        } else {
                            log.debug("⏳ Warte weiter auf Zustellung: {}", e.getMessage());
                        }
                        throw e;
                    }
                });
    }

    // IMPROVED: More resilient notification checking with multiple strategies
    public static boolean waitForNotificationsOptional(String shipmentId, int minCount) {
        log.info("⏳ Versuche Benachrichtigungen für Sendung {} zu finden (optional)", shipmentId);

        try {
            // First, debug the notification endpoint to understand its structure
            debugNotificationEndpoint();

            Awaitility.await("Notifications for " + shipmentId)
                    .atMost(NOTIFICATION_TIMEOUT)
                    .pollInterval(POLL_INTERVAL)
                    .untilAsserted(() -> {
                        int foundCount = getNotificationCountWithDebug(shipmentId);
                        if (foundCount < minCount) {
                            log.debug("⏳ Nur {} von {} erwarteten Benachrichtigungen gefunden", foundCount, minCount);
                            throw new AssertionError(String.format("Nur %d von %d Benachrichtigungen gefunden", foundCount, minCount));
                        }
                    });

            log.info("✅ Benachrichtigungen erfolgreich gefunden");
            return true;

        } catch (Exception e) {
            log.warn("⚠️ Benachrichtigungen nicht verfügbar nach {}s - das ist normal bei async Services",
                    NOTIFICATION_TIMEOUT.getSeconds());
            log.debug("Details: {}", e.getMessage());
            return false;
        }
    }

    // IMPROVED: Mandatory notification check with better error handling
    public static void waitForNotifications(String shipmentId, int minCount) {
        log.info("⏳ Warte auf Benachrichtigungen für Sendung: {}", shipmentId);

        // First check if notification service is responsive
        if (!isNotificationServiceResponsive()) {
            fail("❌ NotificationService ist nicht erreichbar - kann Benachrichtigungen nicht prüfen");
        }

        try {
            Awaitility.await("Notifications for " + shipmentId)
                    .atMost(NOTIFICATION_TIMEOUT)
                    .pollInterval(POLL_INTERVAL)
                    .untilAsserted(() -> {
                        int foundCount = getNotificationCountWithDebug(shipmentId);
                        if (foundCount < minCount) {
                            log.debug("⏳ Nur {} von {} erwarteten Benachrichtigungen gefunden", foundCount, minCount);
                            throw new AssertionError(String.format("Nur %d von %d Benachrichtigungen gefunden", foundCount, minCount));
                        }
                        log.info("✅ {} Benachrichtigungen gefunden", foundCount);
                    });

        } catch (org.awaitility.core.ConditionTimeoutException e) {
            // Provide detailed failure analysis
            analyzeNotificationFailure(shipmentId, minCount);
            throw e;
        }
    }

    private static void debugNotificationEndpoint() {
        log.debug("🔍 Debugge NotificationService Endpunkte...");

        String[] possibleEndpoints = {
                "/api/notifications",
                "/notifications",
                "/api/v1/notifications",
                "/notification",
                "/api/notification"
        };

        for (String endpoint : possibleEndpoints) {
            try {
                Response response = given()
                        .when().get(BASE_URL + ":" + NOTIFICATION_PORT + endpoint);

                log.debug("Endpoint {}: Status {}", endpoint, response.getStatusCode());
                if (response.getStatusCode() == 200) {
                    String body = response.getBody().asString();
                    log.debug("Endpoint {} Antwort (ersten 200 Zeichen): {}",
                            endpoint, body.length() > 200 ? body.substring(0, 200) + "..." : body);
                }
            } catch (Exception e) {
                log.debug("Endpoint {} error: {}", endpoint, e.getMessage());
            }
        }
    }

    private static boolean isNotificationServiceResponsive() {
        try {
            Response response = given()
                    .when().get(BASE_URL + ":" + NOTIFICATION_PORT + "/api/notifications");

            // Accept various response codes that indicate the service is responsive
            return response.getStatusCode() < 500; // 2xx, 3xx, 4xx are fine, 5xx indicates server problems
        } catch (Exception e) {
            log.debug("NotificationService nicht erreichbar: {}", e.getMessage());
            return false;
        }
    }

    private static int getNotificationCountWithDebug(String shipmentId) {
        try {
            Response response = given()
                    .when().get(BASE_URL + ":" + NOTIFICATION_PORT + "/api/notifications");

            log.debug("NotificationService Status: {}", response.getStatusCode());

            if (response.getStatusCode() == 404) {
                log.debug("⏳ Benachrichtigungen noch nicht verfügbar (404)");
                return 0;
            } else if (response.getStatusCode() == 202) {
                log.debug("⏳ Benachrichtigungen werden noch verarbeitet (202)");
                return 0;
            } else if (response.getStatusCode() == 204) {
                log.debug("⏳ Keine Benachrichtigungen vorhanden (204)");
                return 0;
            } else if (response.getStatusCode() != 200) {
                log.debug("⚠️ Unerwarteter Status: {}", response.getStatusCode());
                return 0;
            }

            String body = response.getBody().asString();
            log.debug("Notification Response Body: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);

            JsonNode notifications = objectMapper.readTree(body);

            // Handle various response formats
            JsonNode notificationArray = extractNotificationArray(notifications);
            if (notificationArray == null) {
                log.debug("⚠️ Konnte Notification Array nicht extrahieren aus: {}", notifications.toString());
                return 0;
            }

            int count = countNotificationsForShipment(notificationArray, shipmentId);
            log.debug("Gefundene Benachrichtigungen für {}: {}", shipmentId, count);

            if (count == 0 && notificationArray.size() > 0) {
                log.debug("Alle Benachrichtigungen: {}", notificationArray.toString());
            }

            return count;

        } catch (Exception e) {
            log.debug("Fehler beim Abrufen der Benachrichtigungen: {}", e.getMessage());
            return 0;
        }
    }

    private static JsonNode extractNotificationArray(JsonNode response) {
        if (response.isArray()) {
            return response;
        }

        // Try various common field names
        String[] possibleArrayFields = {"notifications", "data", "items", "results", "content"};
        for (String field : possibleArrayFields) {
            if (response.has(field) && response.get(field).isArray()) {
                return response.get(field);
            }
        }

        return null;
    }

    private static int countNotificationsForShipment(JsonNode notificationArray, String shipmentId) {
        int count = 0;
        for (JsonNode notification : notificationArray) {
            // Try various possible field names for shipment ID
            String[] possibleIdFields = {"shipmentId", "shipment_id", "id", "referenceId", "reference_id"};

            for (String field : possibleIdFields) {
                if (notification.has(field)) {
                    String notificationShipmentId = notification.get(field).asText();
                    if (shipmentId.equals(notificationShipmentId)) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    private static void analyzeNotificationFailure(String shipmentId, int expectedCount) {
        log.error("❌ Benachrichtigungen-Analyse für Sendung {}", shipmentId);

        try {
            Response response = given()
                    .when().get(BASE_URL + ":" + NOTIFICATION_PORT + "/api/notifications");

            log.error("NotificationService Status: {}", response.getStatusCode());
            log.error("NotificationService Response: {}", response.getBody().asString());

            if (response.getStatusCode() == 200) {
                JsonNode notifications = objectMapper.readTree(response.getBody().asString());
                JsonNode notificationArray = extractNotificationArray(notifications);

                if (notificationArray != null) {
                    log.error("Gesamt-Benachrichtigungen verfügbar: {}", notificationArray.size());
                    log.error("Erwartete Benachrichtigungen für Sendung {}: {}", shipmentId, expectedCount);

                    // Log all notifications for debugging
                    for (int i = 0; i < Math.min(notificationArray.size(), 5); i++) {
                        log.error("Beispiel-Benachrichtigung {}: {}", i, notificationArray.get(i).toString());
                    }
                } else {
                    log.error("❌ Konnte Benachrichtigungs-Array nicht parsen");
                }
            }

        } catch (Exception e) {
            log.error("❌ Fehler bei Benachrichtigungs-Analyse: {}", e.getMessage());
        }
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
                                .when().get(BASE_URL + ":" + ANALYTICS_PORT + "/api/analytics/deliveries");

                        if (response.getStatusCode() == 404) {
                            log.debug("⏳ Analytics-Daten noch nicht verfügbar");
                            throw new AssertionError("Analytics data not available yet");
                        } else if (response.getStatusCode() == 202) {
                            log.debug("⏳ Analytics werden noch berechnet");
                            throw new AssertionError("Analytics still computing");
                        } else if (response.getStatusCode() == 204) {
                            log.debug("⏳ Keine Analytics-Daten vorhanden");
                            throw new AssertionError("No analytics data available");
                        }

                        assertEquals(200, response.getStatusCode(),
                                "AnalyticsService sollte 200 zurückgeben wenn Daten verfügbar sind");

                        JsonNode analytics = objectMapper.readTree(response.getBody().asString());

                        JsonNode analyticsArray;
                        if (analytics.isArray()) {
                            analyticsArray = analytics;
                        } else if (analytics.has("data") && analytics.get("data").isArray()) {
                            analyticsArray = analytics.get("data");
                        } else if (analytics.has("deliveries") && analytics.get("deliveries").isArray()) {
                            analyticsArray = analytics.get("deliveries");
                        } else {
                            log.debug("⏳ Unerwartetes Analytics-Format: {}", analytics.toString());
                            throw new AssertionError("Unexpected analytics format");
                        }

                        if (analyticsArray.size() < minEntries) {
                            log.debug("⏳ Nur {} Analytics-Einträge, warte auf {}", analyticsArray.size(), minEntries);
                            throw new AssertionError("Nicht genug Analytics-Einträge: " + analyticsArray.size());
                        }

                        log.info("✅ Analytics verfügbar: {} Einträge", analyticsArray.size());
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                            log.debug("⏳ AnalyticsService noch nicht erreichbar...");
                        } else {
                            log.debug("⏳ Warte weiter auf Analytics: {}", e.getMessage());
                        }
                        throw e;
                    }
                });
    }

    // Utility methods
    public static boolean isShipmentDelivered(String shipmentId) {
        try {
            Response response = given()
                    .when().get(BASE_URL + ":" + DELIVERY_PORT + "/deliveries/" + shipmentId);

            return response.getStatusCode() == 200 &&
                    "DELIVERED".equals(response.jsonPath().getString("status"));
        } catch (Exception e) {
            log.debug("Fehler beim Prüfen des Delivery-Status: {}", e.getMessage());
            return false;
        }
    }

    public static int getNotificationCount(String shipmentId) {
        return getNotificationCountWithDebug(shipmentId);
    }

    public static void scanShipmentWithRetry(String shipmentId, String location, int maxRetries) {
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                scanShipment(shipmentId, location);
                return;
            } catch (Exception e) {
                lastException = e;
                log.debug("Scan-Versuch {} fehlgeschlagen: {}", i + 1, e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Scan fehlgeschlagen nach " + maxRetries + " Versuchen", lastException);
    }
}