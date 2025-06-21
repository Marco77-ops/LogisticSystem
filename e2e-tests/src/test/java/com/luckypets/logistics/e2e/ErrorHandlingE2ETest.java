package com.luckypets.logistics.e2e;

import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ErrorHandlingE2ETest {

    private static final String BASE_URL = "http://localhost";

    // Pre-defined JSON strings als Konstanten
    private static final String EMPTY_JSON = "{}";
    private static final String INVALID_JSON = "invalid json";
    private static final String INCOMPLETE_JSON = "{invalid";
    private static final String PARTIAL_SHIPMENT = "{\"origin\":\"Berlin\"}";
    private static final String PARTIAL_SCAN = "{\"location\":\"TestLocation\"}";

    @Test
    @Order(1)
    @DisplayName("Error: Leere Requests")
    void emptyRequests() {
        // Test 1: Leere Sendungsdaten
        given()
                .contentType("application/json")
                .body(EMPTY_JSON)
                .when()
                .post(BASE_URL + ":8081/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        // Test 2: Leere Scan-Daten
        given()
                .contentType("application/json")
                .body(EMPTY_JSON)
                .when()
                .post(BASE_URL + ":8082/api/v1/scans")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        System.out.println("✅ Leere Requests korrekt abgelehnt");
    }

    @Test
    @Order(2)
    @DisplayName("Error: Nicht-existierende IDs")
    void nonExistentIds() {
        // Test: Zustellungsstatus für nicht-existierende Sendung
        given()
                .when()
                .get(BASE_URL + ":8083/deliveries/non-existent-id-12345")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));

        System.out.println("✅ Nicht-existierende IDs korrekt behandelt");
    }

    @Test
    @Order(3)
    @DisplayName("Error: Ungültiges JSON")
    void invalidJson() {
        // Test 1: Komplett ungültiges JSON
        given()
                .contentType("application/json")
                .body(INVALID_JSON)
                .when()
                .post(BASE_URL + ":8081/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        // Test 2: Unvollständiges JSON
        given()
                .contentType("application/json")
                .body(INCOMPLETE_JSON)
                .when()
                .post(BASE_URL + ":8081/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        System.out.println("✅ Ungültiges JSON korrekt abgelehnt");
    }

    @Test
    @Order(4)
    @DisplayName("Error: Service Health nach Fehlern")
    void serviceHealthAfterErrors() {
        // Prüfe, dass Services nach Fehlern weiterhin funktionieren
        given()
                .when()
                .get(BASE_URL + ":8081/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        given()
                .when()
                .get(BASE_URL + ":8083/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        System.out.println("✅ Services sind nach Fehlern weiterhin healthy");
    }

    @Test
    @Order(5)
    @DisplayName("Error: Unvollständige Daten")
    void incompleteData() {
        // Test mit partiellen Sendungsdaten
        given()
                .contentType("application/json")
                .body(PARTIAL_SHIPMENT)
                .when()
                .post(BASE_URL + ":8081/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        // Test mit partiellen Scan-Daten
        given()
                .contentType("application/json")
                .body(PARTIAL_SCAN)
                .when()
                .post(BASE_URL + ":8082/api/v1/scans")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        System.out.println("✅ Unvollständige Daten korrekt abgelehnt");
    }
}