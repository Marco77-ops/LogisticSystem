package com.luckypets.logistics.e2e.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssertions {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void assertHealthy(Response response) {
        assertEquals(200, response.getStatusCode(), "Service sollte healthy sein");
        assertEquals("UP", response.jsonPath().getString("status"), "Status sollte UP sein");
    }

    public static void assertSuccessfulCreation(Response response) {
        assertEquals(201, response.getStatusCode(), "Erstellung sollte erfolgreich sein");
        assertNotNull(response.jsonPath().getString("id"), "ID sollte gesetzt sein");
    }

    public static void assertDelivered(Response response, String expectedShipmentId) {
        assertEquals(200, response.getStatusCode(), "Zustellungsabfrage sollte erfolgreich sein");
        assertEquals("DELIVERED", response.jsonPath().getString("status"), "Status sollte DELIVERED sein");
        assertEquals(expectedShipmentId, response.jsonPath().getString("shipmentId"), "Sendungs-ID sollte übereinstimmen");
    }

    public static void assertNotificationsExist(Response response, String shipmentId, int minCount) throws Exception {
        assertEquals(200, response.getStatusCode(), "Benachrichtigungsabfrage sollte erfolgreich sein");

        JsonNode notifications = objectMapper.readTree(response.getBody().asString());
        assertTrue(notifications.isArray(), "Benachrichtigungen sollten Array sein");

        int count = 0;
        for (JsonNode notification : notifications) {
            if (notification.has("shipmentId") &&
                    shipmentId.equals(notification.get("shipmentId").asText())) {
                count++;
            }
        }

        assertTrue(count >= minCount,
                String.format("Erwartet mindestens %d Benachrichtigungen für %s, aber nur %d gefunden",
                        minCount, shipmentId, count));
    }

    public static void assertAnalyticsContainLocation(Response response, String location, int minCount) throws Exception {
        assertEquals(200, response.getStatusCode(), "Analytics-Abfrage sollte erfolgreich sein");

        JsonNode analytics = objectMapper.readTree(response.getBody().asString());
        assertTrue(analytics.isArray(), "Analytics sollten Array sein");

        int totalCount = 0;
        for (JsonNode analytic : analytics) {
            if (analytic.has("location") && location.equals(analytic.get("location").asText())) {
                if (analytic.has("count")) {
                    totalCount += analytic.get("count").asInt();
                }
            }
        }

        assertTrue(totalCount >= minCount,
                String.format("Erwartet mindestens %d Analytics-Einträge für %s, aber nur %d gefunden",
                        minCount, location, totalCount));
    }
}