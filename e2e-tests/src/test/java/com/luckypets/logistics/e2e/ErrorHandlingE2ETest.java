package com.luckypets.logistics.e2e;

import com.luckypets.logistics.e2e.utils.ServiceHealthChecker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import static com.luckypets.logistics.e2e.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class ErrorHandlingE2ETest {

    // Vordefinierte Test-Daten als Konstanten für bessere Lesbarkeit
    private static final String EMPTY_JSON = "{}";
    private static final String INVALID_JSON = "invalid json";
    private static final String INCOMPLETE_JSON = "{\"incomplete\":";
    private static final String MISSING_REQUIRED_FIELDS = "{\"origin\":\"Berlin\"}";
    private static final String INVALID_SHIPMENT_ID = "non-existent-id-12345";

    @BeforeAll
    static void setUp() {
        ServiceHealthChecker.waitForAllServices();
    }

    @Test
    @Order(1)
    @DisplayName("Error: Ungültige JSON-Strukturen")
    void invalidJsonStructures() {
        log.info("🧪 Teste ungültige JSON-Strukturen");

        // Test 1: Komplett leeres JSON
        given()
                .contentType("application/json")
                .body(EMPTY_JSON)
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        // Test 2: Ungültiges JSON Format
        given()
                .contentType("application/json")
                .body(INVALID_JSON)
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        // Test 3: Unvollständiges JSON
        given()
                .contentType("application/json")
                .body(INCOMPLETE_JSON)
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        log.info("✅ Ungültige JSON-Strukturen korrekt abgelehnt");
    }

    @Test
    @Order(2)
    @DisplayName("Error: Fehlende Pflichtfelder")
    void missingRequiredFields() {
        log.info("🧪 Teste fehlende Pflichtfelder");

        // Sendung mit fehlenden Feldern
        given()
                .contentType("application/json")
                .body(MISSING_REQUIRED_FIELDS)
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        // Scan mit fehlender shipmentId
        given()
                .contentType("application/json")
                .body("{\"location\":\"TestLocation\"}")
                .when().post(BASE_URL + ":" + SCAN_PORT + "/api/v1/scans")
                .then().statusCode(anyOf(equalTo(400), equalTo(422)));

        log.info("✅ Fehlende Pflichtfelder korrekt abgelehnt");
    }

    @Test
    @Order(3)
    @DisplayName("Error: Nicht-existierende Ressourcen")
    void nonExistentResources() {
        log.info("🧪 Teste nicht-existierende Ressourcen");

        // Test 1: Zustellungsstatus für nicht-existierende Sendung
        given()
                .when().get(BASE_URL + ":" + DELIVERY_PORT + "/deliveries/" + INVALID_SHIPMENT_ID)
                .then().statusCode(anyOf(equalTo(400), equalTo(404)));

        // Test 2: Scan für nicht-existierende Sendung (sollte trotzdem 201 zurückgeben)
        given()
                .contentType("application/json")
                .body(String.format("""
                {
                    "shipmentId": "%s",
                    "location": "TestLocation"
                }
                """, INVALID_SHIPMENT_ID))
                .when().post(BASE_URL + ":" + SCAN_PORT + "/api/v1/scans")
                .then().statusCode(201); // ScanService akzeptiert alle Scans

        log.info("✅ Nicht-existierende Ressourcen korrekt behandelt");
    }

    @Test
    @Order(4)
    @DisplayName("Error: Service-Resilience nach Fehlern")
    void serviceResilienceAfterErrors() {
        log.info("🧪 Teste Service-Resilience nach Fehlern");

        // Mehrere fehlerhafte Requests senden
        for (int i = 0; i < 5; i++) {
            given()
                    .contentType("application/json")
                    .body(INVALID_JSON)
                    .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                    .then().statusCode(anyOf(equalTo(400), equalTo(422)));
        }

        // Services sollten weiterhin healthy sein
        Assertions.assertDoesNotThrow(() -> {
            ServiceHealthChecker.waitForAllServices();
        }, "Services sollten nach Fehlern weiterhin erreichbar sein");

        // Normaler Request sollte weiterhin funktionieren
        given()
                .contentType("application/json")
                .body("""
                {
                    "origin": "ErrorTestOrigin",
                    "destination": "ErrorTestDestination",
                    "customerId": "error-test-customer"
                }
                """)
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(201)
                .body("id", notNullValue());

        log.info("✅ Services sind nach Fehlern weiterhin funktionsfähig");
    }

    @Test
    @Order(5)
    @DisplayName("Error: Verschiedene Content-Types")
    void invalidContentTypes() {
        log.info("🧪 Teste verschiedene ungültige Content-Types");

        // Test 1: Ohne Content-Type
        given()
                .body("{\"origin\":\"Test\"}")
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(415)));

        // Test 2: Falscher Content-Type
        given()
                .contentType("text/plain")
                .body("{\"origin\":\"Test\"}")
                .when().post(BASE_URL + ":" + SHIPMENT_PORT + "/api/v1/shipments")
                .then().statusCode(anyOf(equalTo(400), equalTo(415)));

        log.info("✅ Ungültige Content-Types korrekt abgelehnt");
    }
}