package com.luckypets.logistics.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckypets.logistics.e2e.config.E2ETestConfiguration;
import com.luckypets.logistics.e2e.model.ShipmentRequest;
import com.luckypets.logistics.e2e.model.ScanRequest;
import com.luckypets.logistics.e2e.utils.ApiClient;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringJUnitConfig(TestConfiguration.class)
public class BasicWorkflowE2ETest {

    @Container
    static ComposeContainer environment = new ComposeContainer(
            new File("../docker-compose.test.yml"))
            .withExposedService("shipmentservice", 8081,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("scanservice", 8082,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("deliveryservice", 8083,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("analyticservice", 8084,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("notificationviewservice", 8085,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)));

    private static ApiClient apiClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        String dockerHost = environment.getServiceHost("shipmentservice", 8081);

        apiClient = ApiClient.builder()
                .shipmentPort(environment.getServicePort("shipmentservice", 8081))
                .scanPort(environment.getServicePort("scanservice", 8082))
                .deliveryPort(environment.getServicePort("deliveryservice", 8083))
                .analyticsPort(environment.getServicePort("analyticservice", 8084))
                .notificationPort(environment.getServicePort("notificationviewservice", 8085))
                .host(dockerHost)
                .build();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Order(1)
    @DisplayName("System Health Check - Alle Services sind erreichbar")
    void systemHealthCheck() {
        assertAll("Health Checks",
                () -> apiClient.checkShipmentServiceHealth(),
                () -> apiClient.checkScanServiceHealth(),
                () -> apiClient.checkDeliveryServiceHealth(),
                () -> apiClient.checkAnalyticsServiceHealth(),
                () -> apiClient.checkNotificationServiceHealth()
        );
    }

    @Test
    @Order(2)
    @DisplayName("Vollständiger Sendungsworkflow: Erstellung → Scan → Zustellung → Benachrichtigung")
    void completeShipmentWorkflow() {
        // 1. Sendung erstellen
        ShipmentRequest shipmentRequest = ShipmentRequest.builder()
                .origin("Berlin")
                .destination("Munich")
                .customerId("e2e-customer-123")
                .build();

        Response shipmentResponse = apiClient.createShipment(shipmentRequest);
        String shipmentId = shipmentResponse.jsonPath().getString("id");

        assertNotNull(shipmentId, "Sendungs-ID sollte nicht null sein");
        System.out.println("✅ Sendung erstellt: " + shipmentId);

        // 2. Sendung am Ursprungsort scannen
        ScanRequest originScan = ScanRequest.builder()
                .shipmentId(shipmentId)
                .location("Berlin")
                .build();

        apiClient.scanShipment(originScan);
        System.out.println("✅ Sendung am Ursprungsort gescannt");

        // 3. Sendung am Zielort scannen (löst Zustellung aus)
        ScanRequest destinationScan = ScanRequest.builder()
                .shipmentId(shipmentId)
                .location("Munich")
                .build();

        apiClient.scanShipment(destinationScan);
        System.out.println("✅ Sendung am Zielort gescannt");

        // 4. Warten auf Event-Processing und Zustellungsstatus prüfen
        await("Zustellungsstatus")
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Response deliveryResponse = apiClient.getDeliveryStatus(shipmentId);
                    assertEquals("DELIVERED", deliveryResponse.jsonPath().getString("status"));
                });
        System.out.println("✅ Zustellung bestätigt");

        // 5. Benachrichtigungen prüfen
        await("Benachrichtigungen")
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Response notificationResponse = apiClient.getNotifications();
                    JsonNode notifications = objectMapper.readTree(notificationResponse.getBody().asString());

                    assertTrue(notifications.isArray(), "Benachrichtigungen sollten ein Array sein");

                    long shipmentNotificationCount = 0;
                    for (JsonNode notification : notifications) {
                        if (notification.has("shipmentId") &&
                                shipmentId.equals(notification.get("shipmentId").asText())) {
                            shipmentNotificationCount++;
                        }
                    }

                    assertTrue(shipmentNotificationCount >= 3,
                            "Mindestens 3 Benachrichtigungen für Sendung " + shipmentId + " erwartet, aber nur " + shipmentNotificationCount + " gefunden");
                });
        System.out.println("✅ Benachrichtigungen validiert");

        // 6. Analytics prüfen
        await("Analytics")
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    Response analyticsResponse = apiClient.getAnalytics();
                    JsonNode analytics = objectMapper.readTree(analyticsResponse.getBody().asString());

                    assertTrue(analytics.isArray() && analytics.size() > 0,
                            "Analytics sollten Daten enthalten");

                    // Prüfe, ob Munich-Zustellung in Analytics vorhanden
                    boolean foundMunichDelivery = false;
                    for (JsonNode analytic : analytics) {
                        if (analytic.has("location") && "Munich".equals(analytic.get("location").asText())) {
                            foundMunichDelivery = true;
                            break;
                        }
                    }
                    assertTrue(foundMunichDelivery, "Munich-Zustellung sollte in Analytics sichtbar sein");
                });
        System.out.println("✅ Analytics validiert");

        System.out.println("🎉 Kompletter Workflow erfolgreich abgeschlossen für Sendung: " + shipmentId);
    }

    @Test
    @Order(3)
    @DisplayName("Mehrere parallele Sendungen")
    void multipleParallelShipments() {
        String[] destinations = {"Hamburg", "Frankfurt", "Cologne", "Stuttgart"};
        String[] shipmentIds = new String[destinations.length];

        // Parallel Sendungen erstellen und scannen
        for (int i = 0; i < destinations.length; i++) {
            ShipmentRequest request = ShipmentRequest.builder()
                    .origin("Berlin")
                    .destination(destinations[i])
                    .customerId("parallel-customer-" + i)
                    .build();

            Response response = apiClient.createShipment(request);
            shipmentIds[i] = response.jsonPath().getString("id");

            // Sofort am Zielort scannen
            ScanRequest scan = ScanRequest.builder()
                    .shipmentId(shipmentIds[i])
                    .location(destinations[i])
                    .build();

            apiClient.scanShipment(scan);
        }

        // Alle Zustellungen validieren
        for (int i = 0; i < destinations.length; i++) {
            final int index = i;
            await("Zustellung " + destinations[i])
                    .atMost(Duration.ofSeconds(20))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        Response deliveryResponse = apiClient.getDeliveryStatus(shipmentIds[index]);
                        assertEquals("DELIVERED", deliveryResponse.jsonPath().getString("status"));
                    });
        }

        System.out.println("✅ Alle parallelen Sendungen erfolgreich zugestellt");
    }
}