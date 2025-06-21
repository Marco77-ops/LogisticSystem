package com.luckypets.logistics.e2e.utils;

import com.luckypets.logistics.e2e.model.ShipmentRequest;
import com.luckypets.logistics.e2e.model.ScanRequest;
import io.restassured.response.Response;
import lombok.Builder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Builder
public class ApiClient {

    private final String host;
    private final Integer shipmentPort;
    private final Integer scanPort;
    private final Integer deliveryPort;
    private final Integer analyticsPort;
    private final Integer notificationPort;

    // Service URLs
    private String getShipmentServiceUrl() {
        return "http://" + host + ":" + shipmentPort;
    }

    private String getScanServiceUrl() {
        return "http://" + host + ":" + scanPort;
    }

    private String getDeliveryServiceUrl() {
        return "http://" + host + ":" + deliveryPort;
    }

    private String getAnalyticsServiceUrl() {
        return "http://" + host + ":" + analyticsPort;
    }

    private String getNotificationServiceUrl() {
        return "http://" + host + ":" + notificationPort;
    }

    // Health Checks
    public void checkShipmentServiceHealth() {
        given()
                .when().get(getShipmentServiceUrl() + "/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    public void checkScanServiceHealth() {
        given()
                .when().get(getScanServiceUrl() + "/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    public void checkDeliveryServiceHealth() {
        given()
                .when().get(getDeliveryServiceUrl() + "/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    public void checkAnalyticsServiceHealth() {
        given()
                .when().get(getAnalyticsServiceUrl() + "/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    public void checkNotificationServiceHealth() {
        given()
                .when().get(getNotificationServiceUrl() + "/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    // Shipment Operations
    public Response createShipment(ShipmentRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments")
                .then()
                .statusCode(201)
                .extract().response();
    }

    public Response createShipmentRaw(String json) {
        return given()
                .contentType("application/json")
                .body(json)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments");
    }

    // Scan Operations
    public Response scanShipment(ScanRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans")
                .then()
                .statusCode(201)
                .extract().response();
    }

    public Response scanShipmentRaw(ScanRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans");
    }

    public Response scanShipmentRaw(String json) {
        return given()
                .contentType("application/json")
                .body(json)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans");
    }

    // Delivery Operations
    public Response getDeliveryStatus(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/deliveries/" + shipmentId)
                .then()
                .statusCode(200)
                .extract().response();
    }

    public Response getDeliveryStatusRaw(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/deliveries/" + shipmentId);
    }

    // Analytics Operations
    public Response getAnalytics() {
        return given()
                .when()
                .get(getAnalyticsServiceUrl() + "/api/analytics/deliveries")
                .then()
                .statusCode(200)
                .extract().response();
    }

    // Notification Operations
    public Response getNotifications() {
        return given()
                .when()
                .get(getNotificationServiceUrl() + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();
    }
}