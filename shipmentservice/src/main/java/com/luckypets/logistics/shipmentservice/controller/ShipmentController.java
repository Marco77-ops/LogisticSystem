package com.luckypets.logistics.shipmentservice.controller;

import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;
import com.luckypets.logistics.shipmentservice.service.ShipmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentEntity> createShipment(@Valid @RequestBody ShipmentRequest request) {
        logger.info("Creating shipment with origin: {} and destination: {}", request.getOrigin(), request.getDestination());
        ShipmentEntity createdShipment = shipmentService.createShipment(request);
        return new ResponseEntity<>(createdShipment, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentEntity> getShipmentById(@PathVariable("id") String shipmentId) {
        logger.info("Fetching shipment with ID: {}", shipmentId);
        Optional<ShipmentEntity> shipment = shipmentService.getShipmentById(shipmentId);
        return shipment
                .map(s -> new ResponseEntity<>(s, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<ShipmentEntity>> getAllShipments() {
        logger.info("Fetching all shipments");
        List<ShipmentEntity> shipments = shipmentService.getAllShipments();
        return new ResponseEntity<>(shipments, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable("id") String shipmentId) {
        logger.info("Deleting shipment with ID: {}", shipmentId);
        boolean deleted = shipmentService.deleteShipment(shipmentId);
        return deleted
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
