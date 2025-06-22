package com.luckypets.logistics.e2e;

import com.luckypets.logistics.e2e.utils.ServiceHealthChecker;
import com.luckypets.logistics.e2e.utils.WorkflowTestHelper;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.luckypets.logistics.e2e.config.TestConstants.*;

/**
 * Verbesserte BasicWorkflowE2ETest mit standardisierten Timeouts und robustem Error-Handling
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class BasicWorkflowE2ETest {

    @BeforeAll
    static void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        ServiceHealthChecker.waitForAllServices();
    }

    @Test
    @Order(1)
    @DisplayName("System Health Check")
    void systemHealthCheck() {
        log.info("🔍 System Health Check gestartet");

        // Health Check ist bereits in setUp() passiert
        // Hier nur finale Bestätigung
        Assertions.assertDoesNotThrow(() -> {
            ServiceHealthChecker.waitForAllServices();
        }, "Alle Services sollten healthy sein");

        log.info("✅ System Health Check erfolgreich");
    }

    @Test
    @Order(2)
    @DisplayName("Vollständiger Sendungsworkflow")
    void completeShipmentWorkflow() {
        log.info("🚀 Starte vollständigen Sendungsworkflow");

        // 1. Sendung erstellen
        String shipmentId = WorkflowTestHelper.createShipment(
                DEFAULT_ORIGIN, DEFAULT_DESTINATION, DEFAULT_CUSTOMER_PREFIX + "-complete");

        // 2. Am Ursprungsort scannen
        WorkflowTestHelper.scanShipment(shipmentId, DEFAULT_ORIGIN);

        // 3. Am Zielort scannen (löst Zustellung aus)
        WorkflowTestHelper.scanShipment(shipmentId, DEFAULT_DESTINATION);

        // 4. Auf Zustellung warten
        WorkflowTestHelper.waitForDelivery(shipmentId);

        // 5. Benachrichtigungen prüfen
        WorkflowTestHelper.waitForNotifications(shipmentId, 1);

        // 6. Analytics prüfen (optional)
        WorkflowTestHelper.checkAnalytics(1);

        log.info("🎉 Vollständiger Workflow erfolgreich für Sendung: {}", shipmentId);
    }

    @Test
    @Order(3)
    @DisplayName("Parallele Sendungsverarbeitung")
    void parallelShipmentProcessing() {
        log.info("🚀 Starte parallele Sendungsverarbeitung");

        String[] destinations = {"Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Dresden"};
        List<CompletableFuture<String>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(destinations.length);

        try {
            // Parallel Sendungen erstellen und verarbeiten
            for (int i = 0; i < destinations.length; i++) {
                final int index = i;
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String shipmentId = WorkflowTestHelper.createShipment(
                                DEFAULT_ORIGIN, destinations[index],
                                DEFAULT_CUSTOMER_PREFIX + "-parallel-" + index);

                        // Sofort am Zielort scannen
                        WorkflowTestHelper.scanShipment(shipmentId, destinations[index]);

                        return shipmentId;
                    } catch (Exception e) {
                        log.error("Fehler bei paralleler Verarbeitung von Index {}", index, e);
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Alle Sendungen sammeln
            List<String> shipmentIds = new ArrayList<>();
            for (CompletableFuture<String> future : futures) {
                shipmentIds.add(future.join());
            }

            log.info("✅ {} Sendungen parallel erstellt", shipmentIds.size());

            // Zustellungen validieren
            for (String shipmentId : shipmentIds) {
                WorkflowTestHelper.waitForDelivery(shipmentId);
            }

            log.info("🎉 Parallele Verarbeitung erfolgreich: {} Sendungen", destinations.length);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Bulk-Verarbeitung für Analytics")
    void bulkProcessingForAnalytics() {
        log.info("🚀 Starte Bulk-Verarbeitung für Analytics");

        final int bulkSize = 10;
        final String[] testDestinations = {"TestCity1", "TestCity2", "TestCity3"};

        List<String> shipmentIds = new ArrayList<>();

        // Bulk Sendungen erstellen
        for (int i = 0; i < bulkSize; i++) {
            String destination = testDestinations[i % testDestinations.length];
            String shipmentId = WorkflowTestHelper.createShipment(
                    "BulkOrigin", destination,
                    DEFAULT_CUSTOMER_PREFIX + "-bulk-" + i);

            shipmentIds.add(shipmentId);

            // Sofort scannen für schnelle Verarbeitung
            WorkflowTestHelper.scanShipment(shipmentId, destination);

            // Kleine Pause zwischen Sendungen für realistische Simulation
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("✅ {} Bulk-Sendungen erstellt und gescannt", bulkSize);

        // Analytics sollten die Bulk-Daten enthalten
        WorkflowTestHelper.checkAnalytics(bulkSize);

        log.info("🎉 Bulk-Verarbeitung erfolgreich");
    }
}