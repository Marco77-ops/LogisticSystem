package com.luckypets.logistics.scanservice.model;

public class ScanRequest {
    private String shipmentId;
    private String location;


    public ScanRequest() {}

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
