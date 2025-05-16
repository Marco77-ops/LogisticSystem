package com.luckypets.logistics.notificationservice.repository;

import com.luckypets.logistics.notificationservice.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NotificationRepository {
    
    private final ConcurrentHashMap<String, Notification> notifications = new ConcurrentHashMap<>();
    
    public Notification save(Notification notification) {
        notifications.put(notification.getId(), notification);
        return notification;
    }
    
    public List<Notification> findAll() {
        return new ArrayList<>(notifications.values());
    }
    
    public Optional<Notification> findById(String id) {
        return Optional.ofNullable(notifications.get(id));
    }
    
    public List<Notification> findByShipmentId(String shipmentId) {
        return notifications.values().stream()
                .filter(n -> n.getShipmentId().equals(shipmentId))
                .collect(Collectors.toList());
    }
    
    public void deleteById(String id) {
        notifications.remove(id);
    }
    
    public void deleteAll() {
        notifications.clear();
    }
}