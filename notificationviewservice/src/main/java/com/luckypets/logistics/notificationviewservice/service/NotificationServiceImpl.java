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
    }

    @Override
    public Notification save(Notification notification) {
        // Input validation
        if (notification == null) {
            throw new IllegalArgumentException("Notification must not be null");
        }
        if (notification.getShipmentId() == null || notification.getShipmentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification shipmentId must not be null or empty");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message must not be null or empty");
        }
        if (notification.getType() == null) {
            throw new IllegalArgumentException("Notification type must not be null");
        }

        logger.info("Saving notification: {}", notification);

        try {
            Notification saved = repository.save(notification);
            logger.info("Notification saved successfully: {}", saved);

            // Try to send the event with retry mechanism
            sendNotificationEvent(saved);

            return saved;
        } catch (IllegalArgumentException e) {
            logger.error("Validation error saving notification: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error saving notification: {}", notification, e);
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

            logger.info("Sending NotificationSentEvent: {}", event);
            eventProducer.sendNotificationSentEvent(event);
            logger.info("NotificationSentEvent sent successfully with correlation ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Failed to send NotificationSentEvent for notification {} after retry",
                    savedNotification.getId(), e);
            // TODO: Implement dead letter queue for failed events
            // For now, we log the error but don't fail the notification save operation
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @Override
    public List<Notification> findAll() {
        try {
            List<Notification> notifications = repository.findAll();
            logger.debug("Retrieved {} notifications", notifications.size());
            return notifications;
        } catch (Exception e) {
            logger.error("Error retrieving all notifications", e);
            throw new RuntimeException("Failed to retrieve notifications", e);
        }
    }

    @Override
    public Optional<Notification> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("Attempted to find notification with null or empty ID");
            return Optional.empty();
        }

        try {
            Optional<Notification> notification = repository.findById(id.trim());
            if (notification.isPresent()) {
                logger.debug("Found notification with ID: {}", id);
            } else {
                logger.debug("No notification found with ID: {}", id);
            }
            return notification;
        } catch (Exception e) {
            logger.error("Error finding notification by ID: {}", id, e);
            throw new RuntimeException("Failed to find notification by ID", e);
        }
    }

    @Override
    public List<Notification> findByShipmentId(String shipmentId) {
        if (shipmentId == null || shipmentId.trim().isEmpty()) {
            logger.warn("Attempted to find notifications with null or empty shipment ID");
            return List.of();
        }

        try {
            List<Notification> notifications = repository.findByShipmentId(shipmentId.trim());
            logger.debug("Found {} notifications for shipment ID: {}", notifications.size(), shipmentId);
            return notifications;
        } catch (Exception e) {
            logger.error("Error finding notifications by shipment ID: {}", shipmentId, e);
            throw new RuntimeException("Failed to find notifications by shipment ID", e);
        }
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("Attempted to delete notification with null or empty ID");
            throw new IllegalArgumentException("Notification ID must not be null or empty");
        }

        try {
            if (!repository.existsById(id.trim())) {
                logger.warn("Attempted to delete non-existent notification with ID: {}", id);
                throw new IllegalArgumentException("Notification not found with ID: " + id);
            }

            repository.deleteById(id.trim());
            logger.info("Deleted notification with ID: {}", id);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting notification by ID: {}", id, e);
            throw new RuntimeException("Failed to delete notification", e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            long count = repository.count();
            repository.deleteAll();
            logger.info("Deleted {} notifications", count);
        } catch (Exception e) {
            logger.error("Error deleting all notifications", e);
            throw new RuntimeException("Failed to delete all notifications", e);
        }
    }

    /**
     * Helper method for testing - clears the in-memory storage
     */
    public void clearInMemoryStorageForTests() {
        if (repository != null) {
            repository.clearInMemoryStorageForTests();
        }
    }

    /**
     * Get count of all notifications
     * @return the total count of notifications
     */
    public long getNotificationCount() {
        try {
            long count = repository.count();
            logger.debug("Total notification count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting notification count", e);
            throw new RuntimeException("Failed to get notification count", e);
        }
    }
}