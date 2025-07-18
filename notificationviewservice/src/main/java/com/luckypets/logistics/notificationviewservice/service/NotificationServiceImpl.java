package com.luckypets.logistics.notificationviewservice.service;

import com.luckypets.logistics.notificationviewservice.kafka.NotificationSentEventProducer;
import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.repository.NotificationRepository;
import com.luckypets.logistics.shared.events.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final NotificationSentEventProducer eventProducer;

    public NotificationServiceImpl(
            NotificationRepository repository,
            NotificationSentEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        logger.info("🔧 NotificationServiceImpl initialized");
    }

    @Override
    public Notification save(Notification notification) {
        logger.debug("💾 Attempting to save notification...");

        // Input validation
        if (notification == null) {
            logger.error("❌ Validation failed: Notification is null");
            throw new IllegalArgumentException("Notification must not be null");
        }
        if (notification.getShipmentId() == null || notification.getShipmentId().trim().isEmpty()) {
            logger.error("❌ Validation failed: ShipmentId is null or empty");
            throw new IllegalArgumentException("Notification shipmentId must not be null or empty");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            logger.error("❌ Validation failed: Message is null or empty");
            throw new IllegalArgumentException("Notification message must not be null or empty");
        }
        if (notification.getType() == null) {
            logger.error("❌ Validation failed: Type is null");
            throw new IllegalArgumentException("Notification type must not be null");
        }

        logger.info("📝 Saving notification: shipmentId={}, type={}, message='{}'",
                notification.getShipmentId(), notification.getType(),
                notification.getMessage().length() > 50 ?
                        notification.getMessage().substring(0, 50) + "..." : notification.getMessage());

        try {
            Notification saved = repository.save(notification);
            logger.info("✅ Notification saved successfully: id={}, shipmentId={}",
                    saved.getId(), saved.getShipmentId());

            // Log current state
            long totalCount = repository.count();
            logger.info("📊 Total notifications in system: {}", totalCount);

            // Try to send the event with retry mechanism
            sendNotificationEvent(saved);

            return saved;
        } catch (IllegalArgumentException e) {
            logger.error("❌ Validation error saving notification: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Unexpected error saving notification: shipmentId={}, type={}",
                    notification.getShipmentId(), notification.getType(), e);
            throw new RuntimeException("Failed to save notification", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void sendNotificationEvent(Notification savedNotification) {
        try {
            // Generate proper correlation ID using UUID
            String correlationId = UUID.randomUUID().toString();

            NotificationSentEvent event = new NotificationSentEvent(
                    savedNotification.getId(),
                    savedNotification.getShipmentId(),
                    savedNotification.getType().toString(),
                    savedNotification.getMessage(),
                    correlationId,
                    Instant.now(),
                    "v1"
            );

            logger.debug("📤 Sending NotificationSentEvent: id={}, shipmentId={}, correlationId={}",
                    event.getNotificationId(), event.getShipmentId(), correlationId);

            eventProducer.sendNotificationSentEvent(event);

            logger.info("✅ NotificationSentEvent sent successfully: correlationId={}", correlationId);

        } catch (Exception e) {
            logger.error("❌ Failed to send NotificationSentEvent for notification {} after retry",
                    savedNotification.getId(), e);
            // TODO: Implement dead letter queue for failed events

            throw e;
        }
    }

    @Override
    public List<Notification> findAll() {
        try {
            List<Notification> notifications = repository.findAll();
            logger.debug("🔍 Retrieved {} notifications", notifications.size());
            return notifications;
        } catch (Exception e) {
            logger.error("❌ Error retrieving all notifications", e);
            throw new RuntimeException("Failed to retrieve notifications", e);
        }
    }

    @Override
    public Optional<Notification> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("⚠️ Attempted to find notification with null or empty ID");
            return Optional.empty();
        }

        try {
            Optional<Notification> notification = repository.findById(id.trim());
            if (notification.isPresent()) {
                logger.debug("✅ Found notification with ID: {}", id);
            } else {
                logger.debug("❌ No notification found with ID: {}", id);
            }
            return notification;
        } catch (Exception e) {
            logger.error("❌ Error finding notification by ID: {}", id, e);
            throw new RuntimeException("Failed to find notification by ID", e);
        }
    }

    @Override
    public List<Notification> findByShipmentId(String shipmentId) {
        if (shipmentId == null || shipmentId.trim().isEmpty()) {
            logger.warn("⚠️ Attempted to find notifications with null or empty shipment ID");
            return List.of();
        }

        try {
            List<Notification> notifications = repository.findByShipmentId(shipmentId.trim());
            logger.debug("🔍 Found {} notifications for shipment ID: {}", notifications.size(), shipmentId);

            // Log details if notifications found
            if (!notifications.isEmpty()) {
                logger.info("📋 Notifications for shipment {}: {}", shipmentId,
                        notifications.stream()
                                .map(n -> n.getType().toString())
                                .toList());
            }

            return notifications;
        } catch (Exception e) {
            logger.error("❌ Error finding notifications by shipment ID: {}", shipmentId, e);
            throw new RuntimeException("Failed to find notifications by shipment ID", e);
        }
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("⚠️ Attempted to delete notification with null or empty ID");
            throw new IllegalArgumentException("Notification ID must not be null or empty");
        }

        try {
            if (!repository.existsById(id.trim())) {
                logger.warn("⚠️ Attempted to delete non-existent notification with ID: {}", id);
                throw new IllegalArgumentException("Notification not found with ID: " + id);
            }

            repository.deleteById(id.trim());
            logger.info("🗑️ Deleted notification with ID: {}", id);

            long remainingCount = repository.count();
            logger.debug("📊 Remaining notifications after deletion: {}", remainingCount);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error deleting notification by ID: {}", id, e);
            throw new RuntimeException("Failed to delete notification", e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            long count = repository.count();
            repository.deleteAll();
            logger.info("🗑️ Deleted {} notifications", count);

            long remainingCount = repository.count();
            logger.debug("📊 Remaining notifications after clear: {}", remainingCount);
        } catch (Exception e) {
            logger.error("❌ Error deleting all notifications", e);
            throw new RuntimeException("Failed to delete all notifications", e);
        }
    }

    /**
     * Helper method for testing - clears the in-memory storage
     */
    @Override
    public void clearInMemoryStorageForTests() {
        if (repository != null) {
            repository.clearInMemoryStorageForTests();
            logger.debug("🧪 Cleared in-memory storage for tests");
        }
    }

    /**
     * Get count of all notifications
     */
    @Override
    public long getNotificationCount() {
        try {
            long count = repository.count();
            logger.debug("📊 Total notification count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("❌ Error getting notification count", e);
            throw new RuntimeException("Failed to get notification count", e);
        }
    }

    /**
     * Debug method to log current state
     */
    @Override
    public void logCurrentState() {
        try {
            long totalCount = getNotificationCount();
            List<Notification> allNotifications = findAll();

            logger.info("🔍 NOTIFICATION SERVICE STATE:");
            logger.info("   📊 Total Notifications: {}", totalCount);

            if (!allNotifications.isEmpty()) {
                logger.info("   📋 Recent Notifications:");
                allNotifications.stream()
                        .limit(5)
                        .forEach(n -> logger.info("      - {} | {} | {}",
                                n.getShipmentId(), n.getType(), n.getTimestamp()));
            }
        } catch (Exception e) {
            logger.error("❌ Error logging current state", e);
        }
    }
}