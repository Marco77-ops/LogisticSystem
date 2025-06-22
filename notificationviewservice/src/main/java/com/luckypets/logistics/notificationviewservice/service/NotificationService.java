package com.luckypets.logistics.notificationviewservice.service;

import com.luckypets.logistics.notificationviewservice.model.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing notifications stored by the notification service.
 */
public interface NotificationService {

    /**
     * Persist a notification.
     *
     * @param notification the notification to save
     * @return the saved notification
     */
    Notification save(Notification notification);

    /**
     * Retrieve all notifications.
     *
     * @return list of notifications
     */
    List<Notification> findAll();

    /**
     * Find a notification by its id.
     *
     * @param id notification id
     * @return the notification if present
     */
    Optional<Notification> findById(String id);

    /**
     * Find all notifications for a specific shipment.
     *
     * @param shipmentId the shipment id
     * @return list of notifications
     */
    List<Notification> findByShipmentId(String shipmentId);

    /**
     * Delete a notification by id.
     *
     * @param id notification id
     */
    void deleteById(String id);

    /**
     * Delete all stored notifications.
     */
    void deleteAll();

    /**
     * Get the total count of notifications.
     *
     * @return the count of notifications
     */
    long getNotificationCount();

    /**
     * Helper method for testing - clears the in-memory storage.
     * Should only be used in test environments.
     */
    void clearInMemoryStorageForTests();

    /**
     * Debug method to log current state of the notification service.
     * Useful for troubleshooting and monitoring.
     */
    void logCurrentState();
}