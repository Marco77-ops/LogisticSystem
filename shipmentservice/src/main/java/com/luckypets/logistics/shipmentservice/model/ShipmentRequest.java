package com.luckypets.logistics.shipmentservice.model;

import jakarta.validation.constraints.NotBlank;

// Fügen Sie hier ggf. Validierungsannotationen hinzu (z.B. @NotBlank)
public class ShipmentRequest {


    @NotBlank(message = "Origin must not be blank")
    private String origin;

    @NotBlank(message = "Destination must not be blank")
    private String destination;

    @NotBlank(message = "CustomerId must not be blank")
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