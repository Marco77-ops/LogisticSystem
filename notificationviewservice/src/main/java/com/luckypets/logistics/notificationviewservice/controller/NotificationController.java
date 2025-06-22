package com.luckypets.logistics.notificationviewservice.controller;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        logger.debug("🔍 GET /api/notifications - Fetching all notifications");

        try {
            List<Notification> notifications = service.findAll();
            logger.info("📊 Found {} notifications", notifications.size());

            if (notifications.isEmpty()) {
                logger.info("📭 No notifications found - returning empty list");
                return ResponseEntity.ok(notifications); // Return 200 with empty list, not 204
            }

            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("❌ Error fetching all notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable(name = "id") String id) {
        logger.debug("🔍 GET /api/notifications/{} - Fetching notification by ID", id);

        try {
            return service.findById(id)
                    .map(notification -> {
                        logger.debug("✅ Found notification: {}", notification.getId());
                        return ResponseEntity.ok(notification);
                    })
                    .orElseGet(() -> {
                        logger.debug("❌ Notification not found: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("❌ Error fetching notification by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<List<Notification>> getNotificationsByShipmentId(
            @PathVariable(name = "shipmentId") String shipmentId) {
        logger.debug("🔍 GET /api/notifications/shipment/{} - Fetching notifications by shipment ID", shipmentId);

        try {
            List<Notification> notifications = service.findByShipmentId(shipmentId);
            logger.info("📊 Found {} notifications for shipment {}", notifications.size(), shipmentId);

            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("❌ Error fetching notifications for shipment: {}", shipmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getNotificationCount() {
        logger.debug("🔍 GET /api/notifications/count - Getting notification count");

        try {
            long count = service.getNotificationCount();
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("timestamp", System.currentTimeMillis());

            logger.debug("📊 Total notification count: {}", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting notification count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Notification> createTestNotification(
            @RequestParam("shipmentId") String shipmentId,
            @RequestParam("message") String message,
            @RequestParam(value = "type", defaultValue = "SHIPMENT_CREATED") String typeStr) {

        logger.info("🧪 POST /api/notifications/test - Creating test notification for shipment: {}", shipmentId);

        try {
            NotificationType type;
            try {
                type = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("⚠️ Invalid notification type '{}', using SHIPMENT_CREATED", typeStr);
                type = NotificationType.SHIPMENT_CREATED;
            }

            Notification notification = new Notification(shipmentId, message, type);
            Notification saved = service.save(notification);

            logger.info("✅ Test notification created: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid request for test notification: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("❌ Error creating test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        logger.info("🗑️ DELETE /api/notifications/{} - Deleting notification", id);

        try {
            service.deleteById(id);
            logger.info("✅ Notification deleted: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("❌ Notification not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error deleting notification: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllNotifications() {
        logger.info("🗑️ DELETE /api/notifications - Deleting all notifications");

        try {
            long countBefore = service.getNotificationCount();
            service.deleteAll();
            long countAfter = service.getNotificationCount();

            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", countBefore);
            response.put("remainingCount", countAfter);

            logger.info("✅ Deleted {} notifications", countBefore);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error deleting all notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Alternative endpoint that might be called by E2E tests
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getAllNotificationsAlternative() {
        logger.debug("🔍 GET /api/notifications/notifications - Alternative endpoint");
        return getAllNotifications();
    }

    // Health check endpoint specifically for notifications
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getNotificationHealth() {
        logger.debug("🔍 GET /api/notifications/health - Notification service health check");

        try {
            long count = service.getNotificationCount();
            List<Notification> testList = service.findAll();

            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("notificationCount", count);
            health.put("repositoryAccessible", true);
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("❌ Notification health check failed", e);

            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}