package com.luckypets.logistics.shipmentservice.model;

// Fügen Sie hier ggf. Validierungsannotationen hinzu (z.B. @NotBlank)
public class ShipmentRequest {

    private String origin;
    private String destination;
    private String customerId;
    // Fügen Sie weitere Felder hinzu, die für die Erstellung eines Shipments benötigt werden

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}