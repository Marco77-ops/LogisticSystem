package com.luckypets.logistics.notificationviewservice.controller;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final NotificationService service;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    public DebugController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "notification-view-service");
        info.put("version", "1.0.0");
        info.put("kafkaBootstrapServers", kafkaBootstrapServers);
        info.put("consumerGroupId", consumerGroupId);
        info.put("totalNotifications", service.getNotificationCount());

        return ResponseEntity.ok(info);
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Map<String, Object>> getNotificationCounts() {
        List<Notification> allNotifications = service.findAll();

        Map<String, Object> counts = new HashMap<>();
        counts.put("total", allNotifications.size());

        // Count by type
        Map<String, Long> typeCount = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            long count = allNotifications.stream()
                    .mapToLong(n -> n.getType() == type ? 1 : 0)
                    .sum();
            typeCount.put(type.toString(), count);
        }
        counts.put("byType", typeCount);

        // Count by shipment (top 10)
        Map<String, Long> shipmentCount = new HashMap<>();
        allNotifications.forEach(n -> {
            shipmentCount.merge(n.getShipmentId(), 1L, Long::sum);
        });
        counts.put("byShipment", shipmentCount);

        return ResponseEntity.ok(counts);
    }

    @GetMapping("/notifications/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        List<Notification> allNotifications = service.findAll();

        // Sort by timestamp (most recent first) and limit
        List<Notification> recent = allNotifications.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();

        return ResponseEntity.ok(recent);
    }

    @PostMapping("/notifications/test")
    public ResponseEntity<Notification> createTestNotification(
            @RequestParam String shipmentId,
            @RequestParam(defaultValue = "Test notification") String message,
            @RequestParam(defaultValue = "SHIPMENT_CREATED") String type) {

        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            notificationType = NotificationType.SHIPMENT_CREATED;
        }

        Notification notification = new Notification(shipmentId, message, notificationType);
        Notification saved = service.save(notification);

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/notifications/clear")
    public ResponseEntity<Map<String, Object>> clearAllNotifications() {
        long countBefore = service.getNotificationCount();
        service.deleteAll();
        long countAfter = service.getNotificationCount();

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", countBefore);
        result.put("remainingCount", countAfter);
        result.put("success", countAfter == 0);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test service functionality
            long notificationCount = service.getNotificationCount();
            health.put("notificationService", "UP");
            health.put("notificationCount", notificationCount);

            // Test memory storage
            List<Notification> testFind = service.findAll();
            health.put("repository", "UP");
            health.put("repositoryAccessible", true);

            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(health);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("kafka.bootstrap-servers", kafkaBootstrapServers);
        config.put("kafka.consumer.group-id", consumerGroupId);
        config.put("server.port", 8085);

        return ResponseEntity.ok(config);
    }
}