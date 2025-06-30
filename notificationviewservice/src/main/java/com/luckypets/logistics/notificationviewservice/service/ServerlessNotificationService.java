package com.luckypets.logistics.notificationviewservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServerlessNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ServerlessNotificationService.class);
    private final RestTemplate restTemplate;

    @Value("${luckypets.serverless.webhook.url:http://localhost:9001/webhook}")
    private String serverlessWebhookUrl;

    @Value("${luckypets.serverless.enabled:true}")
    private boolean serverlessEnabled;

    public ServerlessNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // KORRIGIERTE Methode mit 5 Parametern (wie im ShipmentEventListener verwendet)
    public void triggerServerlessFunction(String eventType, String shipmentId, String customerId, String origin, String destination) {
        if (!serverlessEnabled) {
            logger.debug("🚀 Serverless disabled - skipping function call");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("shipmentId", shipmentId);
            payload.put("customerId", customerId != null ? customerId : "CUSTOMER-" + shipmentId.substring(Math.max(0, shipmentId.length() - 3)));
            payload.put("origin", origin);
            payload.put("destination", destination);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("correlationId", "serverless-" + System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            logger.info("🚀 TRIGGERING SERVERLESS FUNCTION: {}", eventType);
            logger.info("📦 Payload: {}", payload);

            ResponseEntity<String> response = restTemplate.postForEntity(serverlessWebhookUrl, request, String.class);

            logger.info("✅ SERVERLESS RESPONSE: {} - {}", response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            logger.error("❌ Serverless Function call failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            // Nicht kritisch - bestehende Notifications funktionieren weiter
        }
    }

    // ZUSÄTZLICHE Überladung mit 3 Parametern (falls irgendwo anders verwendet)
    public void triggerServerlessFunction(String eventType, String shipmentId, String destination) {
        triggerServerlessFunction(eventType, shipmentId, null, null, destination);
    }
}