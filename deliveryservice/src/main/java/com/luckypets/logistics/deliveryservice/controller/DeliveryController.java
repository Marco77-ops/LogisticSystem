package com.luckypets.logistics.deliveryservice.controller;

import com.luckypets.logistics.deliveryservice.exception.ShipmentNotFoundException;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final ShipmentRepository repo;

    public DeliveryController(ShipmentRepository repo) {
        this.repo = repo;
    }

    /** Alle Shipments aus der DB zurückliefern */
    @GetMapping
    public List<ShipmentDto> getAll() {
        return repo.findAll().stream()
                .map(entity -> new ShipmentDto(entity.getShipmentId(), entity.getStatus().name()))
                .toList();
    }

    @GetMapping("/{shipmentId}")
    public ShipmentDto getById(@PathVariable String shipmentId) {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId darf nicht null oder leer sein");
        }
        ShipmentEntity shipment = repo.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        return new ShipmentDto(shipment.getShipmentId(), shipment.getStatus().name());


    }

    /** Nur den Status zurückgeben – sauber gekapselt in einem DTO */
    @GetMapping("/{shipmentId}/status")
    public ShipmentStatusDto getStatus(@PathVariable String shipmentId) {
        ShipmentEntity shipment = repo.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        return new ShipmentStatusDto(shipment.getShipmentId(), shipment.getStatus().name());
    }

    public record ShipmentDto(String shipmentId, String status) {}

    /** Einfaches DTO für die Statusantwort */
    public record ShipmentStatusDto(String shipmentId, String status) {}
}