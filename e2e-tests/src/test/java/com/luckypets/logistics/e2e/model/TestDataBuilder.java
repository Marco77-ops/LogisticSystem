package com.luckypets.logistics.e2e.model;

import java.util.Random;

public class TestDataBuilder {

    private static final Random random = new Random();
    private static final String[] ORIGINS = {"Berlin", "Hamburg", "Munich", "Frankfurt", "Cologne"};
    private static final String[] DESTINATIONS = {"Vienna", "Zurich", "Amsterdam", "Paris", "Prague"};

    public static ShipmentRequest createShipmentRequest(String origin, String destination, String customerId) {
        return ShipmentRequest.builder()
                .origin(origin)
                .destination(destination)
                .customerId(customerId)
                .build();
    }

    public static ShipmentRequest createRandomShipmentRequest(String customerPrefix) {
        return ShipmentRequest.builder()
                .origin(ORIGINS[random.nextInt(ORIGINS.length)])
                .destination(DESTINATIONS[random.nextInt(DESTINATIONS.length)])
                .customerId(customerPrefix + "-" + random.nextInt(1000))
                .build();
    }

    public static ScanRequest createScanRequest(String shipmentId, String location) {
        return ScanRequest.builder()
                .shipmentId(shipmentId)
                .location(location)
                .build();
    }

    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}