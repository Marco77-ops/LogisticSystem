package com.luckypets.logistics.notificationviewservice.repository;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NotificationRepository {


    private final ConcurrentHashMap<String, Notification> inMemoryStorage = new ConcurrentHashMap<>();

    /**
     * Save a notification to in-memory storage
     * @param notification the notification to save
     * @return the saved notification
     */
    public Notification save(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification must not be null");
        }
        if (notification.getId() == null || notification.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification ID must not be null or empty");
        }

        inMemoryStorage.put(notification.getId(), notification);
        return notification;
    }

    /**
     * Find all notifications
     * @return list of all notifications
     */
    public List<Notification> findAll() {
        return inMemoryStorage.values().stream().collect(Collectors.toList());
    }

    /**
     * Find notification by ID
     * @param id the notification ID
     * @return Optional containing the notification if found
     */
    public Optional<Notification> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(inMemoryStorage.get(id));
    }

    /**
     * Find all notifications for a specific shipment
     * @param shipmentId the shipment ID
     * @return list of notifications for the shipment
     */
    public List<Notification> findByShipmentId(String shipmentId) {
        if (shipmentId == null || shipmentId.trim().isEmpty()) {
            return List.of();
        }

        return inMemoryStorage.values().stream()
                .filter(notification -> shipmentId.equals(notification.getShipmentId()))
                .collect(Collectors.toList());
    }

    /**
     * Delete notification by ID
     * @param id the notification ID
     */
    public void deleteById(String id) {
        if (id != null && !id.trim().isEmpty()) {
            inMemoryStorage.remove(id);
        }
    }

    /**
     * Delete all notifications
     */
    public void deleteAll() {
        inMemoryStorage.clear();
    }

    /**
     * Check if a notification exists by ID
     * @param id the notification ID
     * @return true if notification exists, false otherwise
     */
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return inMemoryStorage.containsKey(id);
    }

    /**
     * Count total number of notifications
     * @return the count of notifications
     */
    public long count() {
        return inMemoryStorage.size();
    }

    /**
     * Helper method for testing - clears the in-memory storage
     */
    public void clearInMemoryStorageForTests() {
        inMemoryStorage.clear();
    }
}