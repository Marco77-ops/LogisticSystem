package com.luckypets.logistics.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Einfache E2E Tests gegen bereits laufende Services
 * EMPFOHLEN: Beginnen Sie mit dieser Klasse!
 *
 * Voraussetzung: Services müssen laufen (docker-compose up -d)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimpleE2ETest {

    private static final String BASE_URL = "http://localhost";

    @Test
    @Order(1)
    @DisplayName("Health Check - Alle Services sind erreichbar")
    void systemHealthCheck() {
        // Prüfe alle Services
        given().when().get(BASE_URL + ":8081/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        given().when().get(BASE_URL + ":8082/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        given().when().get(BASE_URL + ":8083/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        given().when().get(BASE_URL + ":8084/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        given().when().get(BASE_URL + ":8085/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        System.out.println("✅ Alle Services sind healthy!");
    }

    @Test
    @Order(2)
    @DisplayName("Kompletter Workflow: Sendung → Scan → Zustellung")
    void completeWorkflowTest() {
        // 1. Sendung erstellen
        Response shipmentResponse = given()
                .contentType("application/json")
                .body("""
                {
                    "origin": "TestOrigin",
                    "destination": "TestDestination",
                    "customerId": "test-customer"
                }
                """)
                .when().post(BASE_URL + ":8081/api/v1/shipments")
                .then().statusCode(201)
                .body("id", notNullValue())
                .extract().response();

        String shipmentId = shipmentResponse.jsonPath().getString("id");
        assertNotNull(shipmentId);
        System.out.println("✅ Sendung erstellt: " + shipmentId);

        // 2. Sendung scannen
        given()
                .contentType("application/json")
                .body(String.format("""
                {
                    "shipmentId": "%s",
                    "location": "TestDestination"
                }
                """, shipmentId))
                .when().post(BASE_URL + ":8082/api/v1/scans")
                .then().statusCode(201);
        System.out.println("✅ Sendung gescannt");

        // 3. Zustellung prüfen (mit Retry)
        boolean delivered = false;
        for (int i = 0; i < 10; i++) {
            try {
                given()
                        .when().get(BASE_URL + ":8083/deliveries/" + shipmentId)
                        .then().statusCode(200)
                        .body("status", equalTo("DELIVERED"));
                delivered = true;
                break;
            } catch (AssertionError e) {
                if (i == 9) throw e;
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
        assertTrue(delivered);
        System.out.println("✅ Zustellung bestätigt");

        // 4. Benachrichtigungen prüfen
        given()
                .when().get(BASE_URL + ":8085/api/notifications")
                .then().statusCode(200)
                .body("$", not(empty()));
        System.out.println("✅ Benachrichtigungen vorhanden");

        System.out.println("🎉 Kompletter Workflow erfolgreich!");
    }

    @Test
    @Order(3)
    @DisplayName("Error Handling Test")
    void errorHandlingTest() {
        // Test ungültige Sendung
        given()
                .contentType("application/json")
                .body("{}")
                .when().post(BASE_URL + ":8081/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        System.out.println("✅ Error Handling funktioniert");
    }
}