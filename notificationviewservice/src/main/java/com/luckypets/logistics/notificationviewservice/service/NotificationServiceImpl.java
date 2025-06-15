package com.luckypets.logistics.notificationviewservice.service;

import com.luckypets.logistics.notificationviewservice.kafka.NotificationSentEventProducer;
import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.repository.NotificationRepository;
import com.luckypets.logistics.shared.events.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        logger.info("Saving notification: {}", notification);
        Notification saved = repository.save(notification);
        logger.info("Notification saved successfully: {}", saved);

        try {
            NotificationSentEvent event = new NotificationSentEvent(
                    saved.getId(),
                    saved.getShipmentId(),
                    saved.getType().toString(),
                    saved.getMessage(),
                    "corr-" + saved.getId(),
                    Instant.now(),
                    "v1"
            );

            logger.info("Sending NotificationSentEvent: {}", event);
            eventProducer.sendNotificationSentEvent(event);
            logger.info("NotificationSentEvent sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send NotificationSentEvent for notification {}", saved.getId(), e);
            // Don't throw the exception to avoid failing the notification save
        }

        return saved;
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