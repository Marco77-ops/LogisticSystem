package com.luckypets.logistics.notificationservice.controller;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable(name = "id") String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shipment/{shipmentId}")
    public List<Notification> getNotificationsByShipmentId(@PathVariable(name = "shipmentId") String shipmentId) {
        return repository.findByShipmentId(shipmentId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable(name = "id") String id) {
        if (repository.findById(id).isPresent()) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        repository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
