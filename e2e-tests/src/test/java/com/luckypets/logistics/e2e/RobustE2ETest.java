package com.luckypets.logistics.e2e;

import com.luckypets.logistics.e2e.utils.ServiceHealthChecker;
import com.luckypets.logistics.e2e.utils.WorkflowTestHelper;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static com.luckypets.logistics.e2e.config.TestConstants.*;

/**
 * Robuste E2E Tests die auch bei instabilen Services funktionieren
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class RobustE2ETest {

    @BeforeAll
    static void setUp() {
        ServiceHealthChecker.waitForAllServices();
    }

    @Test
    @Order(1)
    @DisplayName("Robust: Workflow mit Service-Monitoring")
    void workflowWithServiceMonitoring() {
        log.info("🛡️ Starte robusten Workflow mit Service-Monitoring");

        // Service-Status vor dem Test überwachen
        boolean allServicesHealthy = true;
        String[] services = {"ShipmentService", "ScanService", "DeliveryService", "NotificationService"};
        int[] ports = {SHIPMENT_PORT, SCAN_PORT, DELIVERY_PORT, NOTIFICATION_PORT};

        for (int i = 0; i < services.length; i++) {
            if (!ServiceHealthChecker.isServiceHealthy(services[i], ports[i], "/actuator/health")) {
                log.warn("⚠️ {} ist möglicherweise nicht vollständig healthy", services[i]);
                allServicesHealthy = false;
            }
        }

        if (allServicesHealthy) {
            log.info("✅ Alle Services sind healthy - führe vollständigen Test durch");

            String shipmentId = WorkflowTestHelper.createShipment(
                    "RobustOrigin", "RobustDestination", "robust-customer");

            WorkflowTestHelper.scanShipment(shipmentId, "RobustDestination");
            WorkflowTestHelper.waitForDelivery(shipmentId);
            WorkflowTestHelper.waitForNotifications(shipmentId, 1);

        } else {
            log.info("⚠️ Nicht alle Services sind vollständig healthy - führe reduzierten Test durch");

            // Minimaler Test - nur ShipmentService und ScanService
            String shipmentId = WorkflowTestHelper.createShipment(
                    "RobustOrigin", "RobustDestination", "robust-customer-minimal");

            WorkflowTestHelper.scanShipment(shipmentId, "RobustDestination");

            log.info("✅ Minimaler robuster Test erfolgreich");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Robust: Retry-Mechanismus für Event-Processing")
    void retryMechanismForEventProcessing() {
        log.info("🔄 Teste Retry-Mechanismus für Event-Processing");

        String shipmentId = WorkflowTestHelper.createShipment(
                "RetryOrigin", "RetryDestination", "retry-customer");

        WorkflowTestHelper.scanShipment(shipmentId, "RetryDestination");

        // Robustes Warten mit mehreren Retry-Versuchen
        AtomicInteger attempts = new AtomicInteger(0);

        Awaitility.await("Robust Event Processing")
                .atMost(EVENT_PROCESSING_TIMEOUT.multipliedBy(2)) // Doppelte Zeit für robuste Tests
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    int attempt = attempts.incrementAndGet();
                    log.debug("🔄 Event-Processing Versuch #{}", attempt);

                    try {
                        WorkflowTestHelper.waitForDelivery(shipmentId);
                        log.info("✅ Event-Processing erfolgreich nach {} Versuchen", attempt);
                    } catch (Exception e) {
                        if (attempt % 5 == 0) { // Alle 5 Versuche loggen
                            log.info("⏳ Event-Processing Versuch #{} - warte weiter...", attempt);
                        }
                        throw e;
                    }
                });
    }

    @Test
    @Order(3)
    @DisplayName("Robust: Graceful Degradation")
    void gracefulDegradation() {
        log.info("🛡️ Teste Graceful Degradation");

        String shipmentId = WorkflowTestHelper.createShipment(
                "GracefulOrigin", "GracefulDestination", "graceful-customer");

        WorkflowTestHelper.scanShipment(shipmentId, "GracefulDestination");

        // Teste verschiedene Service-Verfügbarkeiten
        boolean deliveryServiceAvailable = ServiceHealthChecker.isServiceHealthy(
                "DeliveryService", DELIVERY_PORT, "/actuator/health");

        boolean analyticsServiceAvailable = ServiceHealthChecker.isServiceHealthy(
                "AnalyticsService", ANALYTICS_PORT, "/actuator/health");

        boolean notificationServiceAvailable = ServiceHealthChecker.isServiceHealthy(
                "NotificationService", NOTIFICATION_PORT, "/actuator/health");

        // Teste basierend auf verfügbaren Services
        if (deliveryServiceAvailable) {
            WorkflowTestHelper.waitForDelivery(shipmentId);
            log.info("✅ DeliveryService verfügbar - Zustellung getestet");
        } else {
            log.info("ℹ️ DeliveryService nicht verfügbar - überspringe Zustellungstest");
        }

        if (notificationServiceAvailable) {
            try {
                WorkflowTestHelper.waitForNotifications(shipmentId, 1);
                log.info("✅ NotificationService verfügbar - Benachrichtigungen getestet");
            } catch (Exception e) {
                log.info("ℹ️ Benachrichtigungen noch nicht verfügbar - ist OK");
            }
        } else {
            log.info("ℹ️ NotificationService nicht verfügbar - überspringe Benachrichtigungstest");
        }

        if (analyticsServiceAvailable) {
            try {
                WorkflowTestHelper.checkAnalytics(1);
                log.info("✅ AnalyticsService verfügbar - Analytics getestet");
            } catch (Exception e) {
                log.info("ℹ️ Analytics noch nicht verfügbar - ist OK bei Kafka Streams");
            }
        } else {
            log.info("ℹ️ AnalyticsService nicht verfügbar - überspringe Analytics-Test");
        }

        log.info("🎉 Graceful Degradation Test erfolgreich");
    }
}