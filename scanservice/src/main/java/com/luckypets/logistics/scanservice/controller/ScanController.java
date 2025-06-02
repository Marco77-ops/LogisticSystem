// scanservice/src/main/java/com/luckypets/logistics/scanservice/controller/ScanController.java
package com.luckypets.logistics.scanservice.controller;

import com.luckypets.logistics.scanservice.model.ScanRequest;
import com.luckypets.logistics.scanservice.model.ScanResult;
import com.luckypets.logistics.scanservice.service.ScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/scans") // jetzt exakt wie im Test!
public class ScanController {
    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    public String scanShipment(@RequestBody ScanRequest request) {
        try {
            ScanResult result = scanService.scanShipment(request.getShipmentId(), request.getLocation());

            if (!result.isSuccess()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getErrorMessage());
            }

            logger.info("Shipment scanned successfully: {}", request.getShipmentId());
            return "Shipment successfully scanned at " + request.getLocation();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scan request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
