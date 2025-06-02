// scanservice/src/main/java/com/luckypets/logistics/scanservice/controller/ScanController.java
package com.luckypets.logistics.scanservice.controller;

import com.luckypets.logistics.scanservice.repository.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/scans")
public class ScanController {

    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);

    private final KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate;
    private final ShipmentRepository repository;

    public ScanController(KafkaTemplate<String, ShipmentScannedEvent> kafkaTemplate,
                          ShipmentRepository repository) {
        this.kafkaTemplate = kafkaTemplate;
        this.repository = repository;
    }

    @PostMapping
    public String scanShipment(@RequestParam("shipmentId") String shipmentId,
                               @RequestParam("location") String location) {

        ShipmentEntity shipment = repository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipment mit ID " + shipmentId + " nicht gefunden"));

        String correlationId = shipment.getShipmentId();

        ShipmentScannedEvent event = new ShipmentScannedEvent(
                shipment.getShipmentId(),
                location,
                LocalDateTime.now(),
                shipment.getDestination(),
                correlationId
        );

        logger.info("Sende ShipmentScannedEvent: {}", event);
        kafkaTemplate.send("shipment-scanned", shipmentId, event);

        return "ShipmentScannedEvent gesendet für ShipmentId: " + shipmentId;
    }
}
