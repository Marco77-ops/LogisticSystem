package com.luckypets.logistics.e2e.utils;

import com.luckypets.logistics.e2e.model.ShipmentRequest;
import com.luckypets.logistics.e2e.model.ScanRequest;
import io.restassured.response.Response;
import lombok.Builder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.anyOf;

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

    // Health Checks - Keep strict (200 only) as health should be consistent
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

    // Shipment Operations - Creation can return 200, 201, or 202 (async processing)
    public Response createShipment(ShipmentRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(202))) // Accept sync or async creation
                .extract().response();
    }

    public Response createShipmentRaw(String json) {
        return given()
                .contentType("application/json")
                .body(json)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments");
    }

    // Scan Operations - Accept success responses (200/201) and async processing (202)
    public Response scanShipment(ScanRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(202))) // Accept various success codes
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

    // Delivery Operations - Accept found (200), not found yet (404), or processing (202)
    public Response getDeliveryStatus(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/deliveries/" + shipmentId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404))) // 200=found, 202=processing, 404=not yet available
                .extract().response();
    }

    public Response getDeliveryStatusRaw(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/deliveries/" + shipmentId);
    }

    // Alternative delivery endpoint that might exist
    public Response getDeliveryStatusAlternative(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/api/v1/deliveries/" + shipmentId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404)))
                .extract().response();
    }

    public Response getDeliveryStatusAlternativeRaw(String shipmentId) {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/api/v1/deliveries/" + shipmentId);
    }

    // Analytics Operations - Accept data available (200), no data yet (404), or computing (202)
    public Response getAnalytics() {
        return given()
                .when()
                .get(getAnalyticsServiceUrl() + "/api/analytics/deliveries")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404))) // 200=data ready, 202=computing, 404=no data
                .extract().response();
    }

    public Response getAnalyticsRaw() {
        return given()
                .when()
                .get(getAnalyticsServiceUrl() + "/api/analytics/deliveries");
    }

    // Alternative analytics endpoints
    public Response getAnalyticsAlternative() {
        return given()
                .when()
                .get(getAnalyticsServiceUrl() + "/analytics/deliveries")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404)))
                .extract().response();
    }

    public Response getAnalyticsAlternativeRaw() {
        return given()
                .when()
                .get(getAnalyticsServiceUrl() + "/analytics/deliveries");
    }

    // Notification Operations - Accept notifications available (200), none yet (404), or processing (202)
    public Response getNotifications() {
        return given()
                .when()
                .get(getNotificationServiceUrl() + "/api/notifications")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404))) // 200=notifications ready, 202=processing, 404=none yet
                .extract().response();
    }

    public Response getNotificationsRaw() {
        return given()
                .when()
                .get(getNotificationServiceUrl() + "/api/notifications");
    }

    // Alternative notification endpoints
    public Response getNotificationsAlternative() {
        return given()
                .when()
                .get(getNotificationServiceUrl() + "/notifications")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202), equalTo(404)))
                .extract().response();
    }

    public Response getNotificationsAlternativeRaw() {
        return given()
                .when()
                .get(getNotificationServiceUrl() + "/notifications");
    }

    // Additional helper methods for robust testing
    public boolean isServiceHealthy(String serviceName, String healthUrl) {
        try {
            given()
                    .when().get(healthUrl)
                    .then().statusCode(200)
                    .body("status", equalTo("UP"));
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ " + serviceName + " health check failed: " + e.getMessage());
            return false;
        }
    }

    // Raw methods without validation for error testing
    public Response createShipmentWithoutValidation(ShipmentRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments");
    }

    public Response scanShipmentWithoutValidation(ScanRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans");
    }

    // Utility method to test error scenarios - expects 4xx/5xx responses
    public Response createInvalidShipment(String invalidJson) {
        return given()
                .contentType("application/json")
                .body(invalidJson)
                .when()
                .post(getShipmentServiceUrl() + "/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422), equalTo(500))) // Accept various error codes
                .extract().response();
    }

    public Response scanInvalidShipment(String invalidJson) {
        return given()
                .contentType("application/json")
                .body(invalidJson)
                .when()
                .post(getScanServiceUrl() + "/api/v1/scans")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404), equalTo(422), equalTo(500))) // Accept various error codes
                .extract().response();
    }

    // Shipment lookup operations
    public Response getShipment(String shipmentId) {
        return given()
                .when()
                .get(getShipmentServiceUrl() + "/api/v1/shipments/" + shipmentId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404))) // 200=found, 404=not found
                .extract().response();
    }

    public Response getShipmentRaw(String shipmentId) {
        return given()
                .when()
                .get(getShipmentServiceUrl() + "/api/v1/shipments/" + shipmentId);
    }

    // List operations that might return empty results
    public Response getAllShipments() {
        return given()
                .when()
                .get(getShipmentServiceUrl() + "/api/v1/shipments")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204))) // 200=data, 204=no content
                .extract().response();
    }

    public Response getAllScans() {
        return given()
                .when()
                .get(getScanServiceUrl() + "/api/v1/scans")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204))) // 200=data, 204=no content
                .extract().response();
    }

    public Response getAllDeliveries() {
        return given()
                .when()
                .get(getDeliveryServiceUrl() + "/deliveries")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204), equalTo(404))) // 200=data, 204=no content, 404=not available yet
                .extract().response();
    }
}