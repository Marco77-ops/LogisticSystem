package com.luckypets.logistics.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsE2ETest {

    private static final String BASE_URL = "http://localhost";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Order(1)
    @DisplayName("Analytics: Health Check")
    void analyticsHealthCheck() {
        given().when().get(BASE_URL + ":8084/actuator/health")
                .then().statusCode(200);
        System.out.println("✅ Analytics Service ist erreichbar");
    }

    @Test
    @Order(2)
    @DisplayName("Analytics: Bulk-Zustellungen Test")
    void bulkDeliveriesAnalytics() throws Exception {
        final String[] destinations = {"Hamburg", "Munich", "Frankfurt"};
        final int shipmentsPerDestination = 2;

        // Sendungen erstellen und scannen
        for (final String destination : destinations) {
            for (int i = 0; i < shipmentsPerDestination; i++) {
                // Sendung erstellen
                Response shipmentResponse = given()
                        .contentType("application/json")
                        .body(String.format("""
                        {
                            "origin": "Origin-%d",
                            "destination": "%s",
                            "customerId": "analytics-customer-%s-%d"
                        }
                        """, i, destination, destination, i))
                        .when().post(BASE_URL + ":8081/api/v1/shipments")
                        .then().statusCode(201)
                        .extract().response();

                String shipmentId = shipmentResponse.jsonPath().getString("id");

                // Sofort scannen für Zustellung
                given()
                        .contentType("application/json")
                        .body(String.format("""
                        {
                            "shipmentId": "%s",
                            "location": "%s"
                        }
                        """, shipmentId, destination))
                        .when().post(BASE_URL + ":8082/api/v1/scans")
                        .then().statusCode(201);

                Thread.sleep(100); // Kurze Pause
            }
        }

        // Warten auf Analytics (einfacher Ansatz)
        Thread.sleep(5000);

        // Analytics prüfen
        Response analyticsResponse = given()
                .when().get(BASE_URL + ":8084/api/analytics/deliveries")
                .then().statusCode(200)
                .extract().response();

        JsonNode analytics = objectMapper.readTree(analyticsResponse.getBody().asString());
        assertTrue(analytics.isArray() && analytics.size() > 0, "Analytics sollten Daten enthalten");

        System.out.println("✅ Analytics Bulk-Test erfolgreich");
    }
}