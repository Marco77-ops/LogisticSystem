package com.luckypets.logistics.notificationviewservice.service;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link NotificationService} using {@link NotificationRepository}.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    @Override
    public List<Notification> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Notification> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<Notification> findByShipmentId(String shipmentId) {
        return repository.findByShipmentId(shipmentId);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
