package com.luckypets.logistics.deliveryservice.controller;

import com.luckypets.logistics.deliveryservice.exception.ShipmentNotFoundException;
import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /** Get all shipments from the database */
    @GetMapping
    public List<DeliveryResponse> getAll() {
        return deliveryService.getAllShipments();
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<DeliveryResponse> getById(@PathVariable String shipmentId) {
        return deliveryService.getShipmentById(shipmentId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
    }

    /** Return only the status as a string */
    @GetMapping("/{shipmentId}/status")
    public String getStatus(@PathVariable String shipmentId) {
        return deliveryService.getShipmentStatus(shipmentId);
    }

    /** Mark a shipment as delivered */
    @PostMapping("/{shipmentId}/deliver")
    public ResponseEntity<DeliveryResponse> markAsDelivered(
            @PathVariable String shipmentId,
            @RequestBody(required = false) DeliveryRequest request) {

        DeliveryRequest deliveryRequest = request != null ? request : new DeliveryRequest();
        deliveryRequest.setShipmentId(shipmentId);

        DeliveryResponse response = deliveryService.markAsDelivered(deliveryRequest);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
