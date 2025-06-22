package com.luckypets.logistics.e2e;

import com.luckypets.logistics.e2e.model.ShipmentRequest;
import com.luckypets.logistics.e2e.model.ScanRequest;
import com.luckypets.logistics.e2e.model.TestDataBuilder;
import com.luckypets.logistics.e2e.utils.ApiClient;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceE2ETest {

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
    }

    @Test
    @Order(1)
    @DisplayName("Performance: Einzelner Sendungsworkflow Durchlaufzeit")
    void singleWorkflowThroughput() {
        Instant start = Instant.now();

        // Sendung erstellen
        ShipmentRequest shipment = TestDataBuilder.createShipmentRequest(
                "PerformanceOrigin",
                "PerformanceDestination",
                "performance-customer"
        );

        Instant shipmentStart = Instant.now();
        Response shipmentResponse = apiClient.createShipment(shipment);
        Duration shipmentDuration = Duration.between(shipmentStart, Instant.now());

        String shipmentId = shipmentResponse.jsonPath().getString("id");
        assertNotNull(shipmentId);

        // Scan durchführen
        Instant scanStart = Instant.now();
        ScanRequest scan = TestDataBuilder.createScanRequest(shipmentId, "PerformanceDestination");
        apiClient.scanShipment(scan);
        Duration scanDuration = Duration.between(scanStart, Instant.now());

        // Zustellung warten
        Instant deliveryStart = Instant.now();
        await("Performance Zustellung")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Response deliveryResponse = apiClient.getDeliveryStatus(shipmentId);
                    assertEquals("DELIVERED", deliveryResponse.jsonPath().getString("status"));
                });
        Duration deliveryDuration = Duration.between(deliveryStart, Instant.now());

        Duration totalDuration = Duration.between(start, Instant.now());

        // Performance-Assertions
        assertTrue(shipmentDuration.toMillis() < 5000,
                "Sendungserstellung sollte unter 5s dauern, war: " + shipmentDuration.toMillis() + "ms");
        assertTrue(scanDuration.toMillis() < 3000,
                "Scan sollte unter 3s dauern, war: " + scanDuration.toMillis() + "ms");
        assertTrue(deliveryDuration.toMillis() < 10000,
                "Zustellung sollte unter 10s dauern, war: " + deliveryDuration.toMillis() + "ms");
        assertTrue(totalDuration.toMillis() < 20000,
                "Gesamter Workflow sollte unter 20s dauern, war: " + totalDuration.toMillis() + "ms");

        System.out.printf("✅ Performance-Metriken: Sendung=%dms, Scan=%dms, Zustellung=%dms, Gesamt=%dms%n",
                shipmentDuration.toMillis(), scanDuration.toMillis(),
                deliveryDuration.toMillis(), totalDuration.toMillis());
    }

    @Test
    @Order(2)
    @DisplayName("Performance: Parallele Sendungsverarbeitung")
    @Timeout(60) // Max 60 Sekunden für Test
    void parallelShipmentProcessing() {
        int numberOfShipments = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<String>> futures = new ArrayList<>();

        Instant start = Instant.now();

        // Parallel Sendungen erstellen
        for (int i = 0; i < numberOfShipments; i++) {
            final int index = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ShipmentRequest shipment = TestDataBuilder.createShipmentRequest(
                            "ParallelOrigin" + index,
                            "ParallelDest" + index,
                            "parallel-customer-" + index
                    );

                    Response response = apiClient.createShipment(shipment);
                    String shipmentId = response.jsonPath().getString("id");

                    // Sofort scannen
                    ScanRequest scan = TestDataBuilder.createScanRequest(shipmentId, "ParallelDest" + index);
                    apiClient.scanShipment(scan);

                    return shipmentId;
                } catch (Exception e) {
                    fail("Parallele Sendungsverarbeitung fehlgeschlagen: " + e.getMessage());
                    return null;
                }
            }, executor);

            futures.add(future);
        }

        // Warten auf alle Sendungen
        List<String> shipmentIds = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                String shipmentId = future.get();
                assertNotNull(shipmentId);
                shipmentIds.add(shipmentId);
            } catch (Exception e) {
                fail("Parallele Verarbeitung fehlgeschlagen: " + e.getMessage());
            }
        }

        Duration creationDuration = Duration.between(start, Instant.now());

        // Alle Zustellungen validieren
        for (String shipmentId : shipmentIds) {
            await("Parallele Zustellung " + shipmentId)
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        Response deliveryResponse = apiClient.getDeliveryStatus(shipmentId);
                        assertEquals("DELIVERED", deliveryResponse.jsonPath().getString("status"));
                    });
        }

        Duration totalDuration = Duration.between(start, Instant.now());

        executor.shutdown();

        // Performance-Validierung
        assertTrue(creationDuration.toMillis() < 30000,
                "Erstellung von " + numberOfShipments + " Sendungen sollte unter 30s dauern, war: " + creationDuration.toMillis() + "ms");
        assertTrue(totalDuration.toMillis() < 60000,
                "Komplette Verarbeitung von " + numberOfShipments + " Sendungen sollte unter 60s dauern, war: " + totalDuration.toMillis() + "ms");

        System.out.printf("✅ Parallele Verarbeitung: %d Sendungen in %dms (Erstellung: %dms)%n",
                numberOfShipments, totalDuration.toMillis(), creationDuration.toMillis());
    }

    @Test
    @Order(3)
    @DisplayName("Performance: System unter Last")
    void systemUnderLoad() {
        int batchSize = 50;
        List<String> shipmentIds = new ArrayList<>();

        Instant start = Instant.now();

        // Burst von Sendungen
        for (int i = 0; i < batchSize; i++) {
            ShipmentRequest shipment = TestDataBuilder.createShipmentRequest(
                    "LoadOrigin",
                    "LoadDest" + (i % 5), // 5 verschiedene Ziele für Analytics
                    "load-customer-" + i
            );

            Response response = apiClient.createShipment(shipment);
            assertEquals(201, response.getStatusCode(), "Sendung " + i + " sollte erfolgreich erstellt werden");

            String shipmentId = response.jsonPath().getString("id");
            shipmentIds.add(shipmentId);

            // Jede 5. Sendung scannen
            if (i % 5 == 0) {
                ScanRequest scan = TestDataBuilder.createScanRequest(shipmentId, "LoadDest" + (i % 5));
                Response scanResponse = apiClient.scanShipmentRaw(scan);
                assertTrue(scanResponse.getStatusCode() < 400, "Scan sollte erfolgreich sein");
            }
        }

        Duration burstDuration = Duration.between(start, Instant.now());

        // System sollte weiterhin responsive sein
        assertDoesNotThrow(() -> {
            apiClient.checkShipmentServiceHealth();
            apiClient.checkScanServiceHealth();
            apiClient.checkDeliveryServiceHealth();
        }, "Services sollten unter Last weiterhin erreichbar sein");

        // Einige Zustellungen sollten verarbeitet werden
        await("System unter Last")
                .atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long deliveredCount = 0;
                    for (String shipmentId : shipmentIds.subList(0, Math.min(10, shipmentIds.size()))) {
                        try {
                            Response deliveryResponse = apiClient.getDeliveryStatusRaw(shipmentId);
                            if (deliveryResponse.getStatusCode() == 200 &&
                                    "DELIVERED".equals(deliveryResponse.jsonPath().getString("status"))) {
                                deliveredCount++;
                            }
                        } catch (Exception e) {
                            // Ignoriere Fehler - einige Sendungen sind möglicherweise noch nicht zugestellt
                        }
                    }

                    assertTrue(deliveredCount > 0, "Mindestens eine Sendung sollte unter Last zugestellt werden");
                });

        System.out.printf("✅ System unter Last: %d Sendungen in %dms verarbeitet%n",
                batchSize, burstDuration.toMillis());
    }
}