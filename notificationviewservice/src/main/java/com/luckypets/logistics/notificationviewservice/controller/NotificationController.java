package com.luckypets.logistics.notificationviewservice.controller;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable(name = "id") String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shipment/{shipmentId}")
    public List<Notification> getNotificationsByShipmentId(@PathVariable(name = "shipmentId") String shipmentId) {
        return service.findByShipmentId(shipmentId);

    }
}
