package com.luckypets.logistics.scanservice.controller;

import com.luckypets.logistics.scanservice.model.ScanRequest;
import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.service.ScanService;
import com.luckypets.logistics.scanservice.service.ScanServiceImpl; // Import ScanServiceImpl
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scans")
public class ScanController {
    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService;
    private final ScanServiceImpl scanServiceImpl; // Inject the implementation to access findById

    public ScanController(ScanService scanService, ScanServiceImpl scanServiceImpl) {
        this.scanService = scanService;
        this.scanServiceImpl = scanServiceImpl;
    }

    @PostMapping
    public ResponseEntity<String> scanShipment(@RequestBody ScanRequest request) {
        try {
            ScanResult result = scanService.scanShipment(request.getShipmentId(), request.getLocation());

            if (!result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getErrorMessage());
            }

            logger.info("Shipment scanned successfully: {}", request.getShipmentId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Shipment successfully scanned at " + request.getLocation());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scan request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred during scan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    // New Endpoint for E2E Test Polling
    // Added a regex to the path variable to ensure it's correctly interpreted as a dynamic path segment.
    // FIX: Explicitly specify the name for @PathVariable to ensure proper mapping.
    @GetMapping("/{shipmentId:^[a-zA-Z0-9-]+$}")
    public ResponseEntity<?> getShipmentStatusForScan(@PathVariable("shipmentId") String shipmentId) {
        if (scanServiceImpl.findById(shipmentId).isPresent()) {
            return ResponseEntity.ok("Shipment " + shipmentId + " found in ScanService memory.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment " + shipmentId + " not yet known to ScanService.");
        }
    }
}
