package com.luckypets.logistics.scanservice.controller;

import com.luckypets.logistics.scanservice.model.ScanRequest;
import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.service.ScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scans")
public class ScanController {
    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    public ResponseEntity<String> scanShipment(@RequestBody ScanRequest request) { // Changed return type to ResponseEntity<String>
        try {
            ScanResult result = scanService.scanShipment(request.getShipmentId(), request.getLocation());

            if (!result.isSuccess()) {
                // If scan fails, return BAD_REQUEST with the error message
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getErrorMessage());
            }

            logger.info("Shipment scanned successfully: {}", request.getShipmentId());
            return ResponseEntity.ok("Shipment successfully scanned at " + request.getLocation()); // Return OK with success message
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scan request: {}", e.getMessage());
            // If IllegalArgumentException, return BAD_REQUEST with the exception message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred during scan: {}", e.getMessage(), e);
            // Catch any other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
