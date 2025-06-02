package com.luckypets.logistics.scanservice.model;

/**
 * Represents the result of a scan operation.
 * Contains information about whether the scan was successful,
 * the shipment ID, and any error message if the scan failed.
 */
public class ScanResult {
    private final boolean success;
    private final String shipmentId;
    private final String errorMessage;

    /**
     * Creates a successful scan result.
     *
     * @param shipmentId the ID of the scanned shipment
     * @return a successful scan result
     */
    public static ScanResult success(String shipmentId) {
        return new ScanResult(true, shipmentId, null);
    }

    /**
     * Creates a failed scan result.
     *
     * @param errorMessage the error message describing why the scan failed
     * @return a failed scan result
     */
    public static ScanResult failure(String errorMessage) {
        return new ScanResult(false, null, errorMessage);
    }

    private ScanResult(boolean success, String shipmentId, String errorMessage) {
        this.success = success;
        this.shipmentId = shipmentId;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the scan was successful.
     *
     * @return true if the scan was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the ID of the scanned shipment.
     * Only valid if the scan was successful.
     *
     * @return the shipment ID, or null if the scan failed
     */
    public String getShipmentId() {
        return shipmentId;
    }

    /**
     * Returns the error message if the scan failed.
     * Only valid if the scan was not successful.
     *
     * @return the error message, or null if the scan was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}