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

    /** Nur den Status zurückgeben als String */
    @GetMapping("/{shipmentId}/status")
    public String getStatus(@PathVariable String shipmentId) {
        return repo.findById(shipmentId)
                .map(shipment -> shipment.getStatus().name())
                .orElse("Unbekannt");
    }

    public record ShipmentDto(String shipmentId, String status) {}
    public record ShipmentStatusDto(String shipmentId, String status) {}
}
