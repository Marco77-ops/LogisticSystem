package com.luckypets.logistics.notificationservice.service;

import com.luckypets.logistics.notificationservice.model.Notification;

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
}
