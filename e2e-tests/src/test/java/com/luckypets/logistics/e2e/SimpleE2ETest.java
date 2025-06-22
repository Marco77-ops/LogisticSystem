package com.luckypets.logistics.e2e;

import com.luckypets.logistics.e2e.utils.ApiClient;
import com.luckypets.logistics.e2e.model.ScanRequest;
import com.luckypets.logistics.e2e.model.ShipmentRequest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.awaitility.Awaitility; // Import Awaitility
import java.time.Duration; // Import Duration

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Funktionierender E2E Test gegen bereits laufende Services
 * KORRIGIERT: Robuste Health-Checks für alle Services
 * KORRIGIERT: Asynchrone Event-Verarbeitung zwischen ShipmentService und ScanService berücksichtigt.
 *
 * Voraussetzung: Services müssen laufen (docker-compose -f docker-compose.test.yml up -d)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimpleE2ETest {

    private static final String BASE_URL = "http://localhost";
    private static ApiClient apiClient; // Declare as static to be initialized once

    @BeforeAll // Use BeforeAll to initialize ApiClient once for all tests
    static void setupApiClient() {
        apiClient = ApiClient.builder()
                .host("localhost")
                .shipmentPort(8081)
                .scanPort(8082)
                .deliveryPort(8083)
                .analyticsPort(8084) // Assuming analyticsPort is 8084 based on application.yml
                .notificationPort(8085)
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Health Check - Services intelligent prüfen")
    void systemHealthCheck() {
        System.out.println("🔍 Starte intelligente Service-Prüfung...");

        // Use ApiClient for consistent health checks
        apiClient.checkShipmentServiceHealth();
        System.out.println("✅ ShipmentService healthy");

        apiClient.checkScanServiceHealth();
        System.out.println("✅ ScanService healthy");

        apiClient.checkDeliveryServiceHealth();
        System.out.println("✅ DeliveryService healthy");

        apiClient.checkAnalyticsServiceHealth();
        System.out.println("✅ AnalyticsService healthy");

        apiClient.checkNotificationServiceHealth();
        System.out.println("✅ NotificationService healthy");

        System.out.println("🎉 Service-Prüfung abgeschlossen!");
    }

    @Test
    @Order(2)
    @DisplayName("Kompletter Workflow: Sendung → Scan → Zustellung")
    void completeWorkflowTest() {
        System.out.println("🚀 Starte kompletten Workflow-Test...");

        // 1. Sendung erstellen
        ShipmentRequest shipmentRequest = new ShipmentRequest();
        shipmentRequest.setOrigin("TestOrigin");
        shipmentRequest.setDestination("TestDestination");
        shipmentRequest.setCustomerId("test-customer");

        Response shipmentResponse = apiClient.createShipment(shipmentRequest);

        // Debug: Response-Inhalt loggen
        System.out.println("📄 ShipmentService Response Body: " + shipmentResponse.getBody().asString());
        System.out.println("📄 ShipmentService Response Status: " + shipmentResponse.getStatusCode());

        // Robuste ID-Extraktion
        String shipmentId = shipmentResponse.jsonPath().getString("shipmentId");
        assertNotNull(shipmentId, "ShipmentId sollte nicht null sein. Response: " + shipmentResponse.getBody().asString());
        System.out.println("✅ Sendung erstellt: " + shipmentId);

        // --- NEU: Warten, bis der ScanService die Sendung über Kafka kennt ---
        System.out.println("⏳ Warte auf ScanService Event-Processing für Shipment ID: " + shipmentId + "...");
        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15)) // Maximal 15 Sekunden warten
                    .pollInterval(Duration.ofMillis(500)) // Alle 500ms prüfen
                    .untilAsserted(() -> {
                        Response scanServiceCheckResponse = apiClient.getScanServiceShipmentStatus(shipmentId);
                        // Erwarte Status 200, wenn die Sendung im ScanService gefunden wurde
                        if (scanServiceCheckResponse.getStatusCode() != 200) {
                            // If not 200, log the full response and fail the assertion to re-poll
                            String errorMessage = String.format(
                                    "Expected status 200, but got %d. Response Body: %s",
                                    scanServiceCheckResponse.getStatusCode(),
                                    scanServiceCheckResponse.getBody().asString()
                            );
                            System.out.println("DEBUG: " + errorMessage); // Debugging-Ausgabe
                            Assertions.fail(errorMessage); // Fail assertion to continue polling
                        }
                        System.out.println("DEBUG: ScanService kennt Sendung " + shipmentId); // Debugging-Ausgabe
                    });
            System.out.println("✅ ScanService hat Sendung " + shipmentId + " verarbeitet.");
        } catch (Exception e) {
            fail("❌ ScanService hat Sendung " + shipmentId + " nicht rechtzeitig verarbeitet: " + e.getMessage());
        }
        // --- ENDE NEU ---

        // 2. Sendung scannen
        ScanRequest scanRequest = new ScanRequest();
        scanRequest.setShipmentId(shipmentId);
        scanRequest.setLocation("TestDestination"); // Scannen am Zielort

        Response scanResponse = apiClient.scanShipment(scanRequest);
        System.out.println("📄 ScanService Response Body: " + scanResponse.getBody().asString());
        System.out.println("📄 ScanService Response Status: " + scanResponse.getStatusCode());
        // Erwarte 201 Created vom ScanService (gemäß ScanController)
        scanResponse.then().statusCode(201);
        System.out.println("✅ Sendung gescannt: " + shipmentId + " an Location: " + scanRequest.getLocation());


        // 3. Zustellung prüfen (robustes Retry)
        System.out.println("⏳ Warte auf DeliveryService Event-Processing und Zustellung von Sendung: " + shipmentId + "...");
        boolean delivered = false;

        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(20)) // Maximal 20 Sekunden warten
                    .pollInterval(Duration.ofMillis(1000)) // Jede Sekunde prüfen
                    .untilAsserted(() -> {
                        Response deliveryResponse = apiClient.getDeliveryStatusRaw(shipmentId);
                        if (deliveryResponse.getStatusCode() == 200) {
                            String status = deliveryResponse.jsonPath().getString("status");
                            if ("DELIVERED".equals(status)) {
                                // Assert final state
                                deliveryResponse.then()
                                        .statusCode(200)
                                        .body("shipmentId", equalTo(shipmentId))
                                        .body("status", equalTo("DELIVERED"))
                                        .body("location", equalTo(scanRequest.getLocation()))
                                        .body("deliveredAt", notNullValue());
                                System.out.println("✅ Zustellung bestätigt: " + status);
                                // Set delivered to true, so outer check passes
                                Assertions.assertTrue(true); // Awaitility needs an assertion to pass
                            } else {
                                System.out.println("⏳ Aktueller DeliveryService Status: " + status + " für " + shipmentId);
                                Assertions.fail("Shipment not yet DELIVERED: " + status); // Fail assertion to continue polling
                            }
                        } else if (deliveryResponse.getStatusCode() == 404) {
                            System.out.println("⏳ DeliveryService kennt Sendung " + shipmentId + " noch nicht. Status: 404");
                            Assertions.fail("Shipment not yet found in DeliveryService (404)"); // Fail assertion to continue polling
                        } else if (deliveryResponse.getStatusCode() == 202) {
                            System.out.println("⏳ DeliveryService verarbeitet Sendung " + shipmentId + ". Status: 202");
                            Assertions.fail("Shipment still processing in DeliveryService (202)"); // Fail assertion to continue polling
                        } else {
                            System.out.println("⚠️ Unerwarteter DeliveryService Status: " + deliveryResponse.getStatusCode() + " für " + shipmentId);
                            System.out.println("📄 DeliveryService Response Body: " + deliveryResponse.getBody().asString());
                            Assertions.fail("Unexpected DeliveryService status: " + deliveryResponse.getStatusCode()); // Fail for unexpected status
                        }
                    });
            delivered = true; // If Awaitility passes, it's delivered
            System.out.println("✅ Zustellung erfolgreich!");
        } catch (Exception e) {
            System.out.println("❌ Zustellung konnte nicht rechtzeitig bestätigt werden für Sendung " + shipmentId + ": " + e.getMessage());
            fail("Zustellung nicht erfolgt oder Timeout: " + e.getMessage());
        }

        // 4. Benachrichtigungen prüfen (optional, da async)
        // This check is often more robust in an integration test of the notification service itself,
        // or a dedicated E2E test focusing on notification flow.
        // For a general workflow, a soft check or just logging is acceptable.
        System.out.println("🔍 Prüfe Benachrichtigungen...");
        try {
            Response notificationsResponse = apiClient.getNotificationsRaw();
            if (notificationsResponse.getStatusCode() == 200) {
                System.out.println("✅ Benachrichtigungen vorhanden. Body: " + notificationsResponse.getBody().asString());
            } else if (notificationsResponse.getStatusCode() == 404) {
                System.out.println("ℹ️ Keine Benachrichtigungen verfügbar (404, normal bei asynchroner Verarbeitung oder keine Benachrichtigungen für diesen Test).");
            } else if (notificationsResponse.getStatusCode() == 204) {
                System.out.println("ℹ️ Keine Benachrichtigungen (204 No Content).");
            } else {
                System.out.println("⚠️ Unerwarteter Status für Benachrichtigungen: " + notificationsResponse.getStatusCode() + ". Body: " + notificationsResponse.getBody().asString());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Fehler beim Abrufen der Benachrichtigungen: " + e.getMessage());
        }

        System.out.println("🎉 Workflow-Test abgeschlossen!");
    }

    @Test
    @Order(3)
    @DisplayName("Error Handling Test")
    void errorHandlingTest() {
        System.out.println("🧪 Teste Error Handling...");

        // Test ungültige Sendung
        apiClient.createInvalidShipment("{}");
        System.out.println("✅ Invalid Shipment Creation Error Handling funktioniert");

        // Test ungültiger Scan (z.B. fehlende Felder)
        apiClient.scanInvalidShipment("{}");
        System.out.println("✅ Invalid Scan Error Handling funktioniert");

        System.out.println("🎉 Error Handling Test abgeschlossen!");
    }

    @Test
    @Order(4)
    @DisplayName("Service-Endpunkte erkunden")
    void exploreServiceEndpoints() {
        System.out.println("🔍 Erkunde Service-Endpunkte...");

        // DeliveryService Endpunkte erkunden
        System.out.println("\n--- DeliveryService Endpunkte ---");
        String[] deliveryEndpoints = {"/deliveries", "/deliveries/123/status", "/actuator/health", "/actuator"};
        for (String endpoint : deliveryEndpoints) {
            try {
                int status = given().when().get(BASE_URL + ":8083" + endpoint).getStatusCode();
                System.out.println("DeliveryService " + endpoint + ": " + status);
            } catch (Exception e) {
                System.out.println("DeliveryService " + endpoint + ": ERROR - " + e.getMessage());
            }
        }

        // AnalyticsService Endpunkte erkunden
        System.out.println("\n--- AnalyticsService Endpunkte ---");
        String[] analyticsEndpoints = {"/api/analytics/deliveries", "/actuator/health", "/actuator"};
        for (String endpoint : analyticsEndpoints) {
            try {
                int status = given().when().get(BASE_URL + ":8084" + endpoint).getStatusCode();
                System.out.println("AnalyticsService " + endpoint + ": " + status);
            } catch (Exception e) {
                System.out.println("AnalyticsService " + endpoint + ": ERROR - " + e.getMessage());
            }
        }

        // NotificationService Endpunkte erkunden
        System.out.println("\n--- NotificationService Endpunkte ---");
        String[] notificationEndpoints = {"/api/notifications", "/actuator/health", "/actuator"};
        for (String endpoint : notificationEndpoints) {
            try {
                int status = given().when().get(BASE_URL + ":8085" + endpoint).getStatusCode();
                System.out.println("NotificationService " + endpoint + ": " + status);
            } catch (Exception e) {
                System.out.println("NotificationService " + endpoint + ": ERROR - " + e.getMessage());
            }
        }

        System.out.println("🔍 Endpunkt-Erkundung abgeschlossen");
    }
}
