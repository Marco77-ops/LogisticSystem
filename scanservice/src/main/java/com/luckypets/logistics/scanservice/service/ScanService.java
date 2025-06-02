package com.luckypets.logistics.scanservice.service;

import com.luckypets.logistics.scanservice.model.ScanResult;

/**
 * Service for handling shipment scan operations.
 * Responsible for validating scan data, updating shipment information,
 * and publishing events when shipments are scanned.
 */
public interface ScanService {

    /**
     * Processes a scan for a shipment at a specific location.
     * 
     * <p>This method:
     * <ul>
     *   <li>Validates the input parameters</li>
     *   <li>Checks if the shipment exists</li>
     *   <li>Updates the shipment's location and last scanned timestamp</li>
     *   <li>Creates and publishes a ShipmentScannedEvent</li>
     * </ul>
     *
     * @param shipmentId the ID of the shipment being scanned (must not be null)
     * @param location the location where the shipment was scanned (must not be empty)
     * @return a ScanResult indicating success or failure
     * @throws IllegalArgumentException if shipmentId is null or location is empty
     */
    ScanResult scanShipment(String shipmentId, String location);
}