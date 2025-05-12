package com.luckypets.logistics.shipmentservice.controller;

import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    private final ShipmentEventProducer producer;

    public ShipmentController(ShipmentEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public String createShipment(@RequestParam("destination") String destination) {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Zieladresse darf nicht leer sein.");
        }

        logger.info("Erzeuge ShipmentCreatedEvent für Ziel: {}", destination);

        String shipmentId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                UUID.randomUUID().toString(),
                destination,
                LocalDateTime.now(),
                correlationId
        );

        producer.sendShipmentCreatedEvent(event);
        return "ShipmentCreatedEvent gesendet für shipmentId: " + shipmentId + " und Ziel: " + destination;
    }
}
