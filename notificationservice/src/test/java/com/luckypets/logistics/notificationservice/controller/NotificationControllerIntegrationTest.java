package com.luckypets.logistics.notificationservice.controller;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.model.NotificationType;
import com.luckypets.logistics.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerIntegrationTest {

    private NotificationRepository repository;
    private NotificationController controller;
    private MockMvc mockMvc;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        repository = new NotificationRepository();
        controller = new NotificationController(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        testNotification = new Notification("test-shipment-id", "Test message", NotificationType.SHIPMENT_CREATED);
        testNotification.setId("test-id");
        repository.save(testNotification);
    }

    @Test
    void getAllNotifications_shouldReturnAllNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testNotification.getId()))
                .andExpect(jsonPath("$[0].shipmentId").value(testNotification.getShipmentId()))
                .andExpect(jsonPath("$[0].message").value(testNotification.getMessage()))
                .andExpect(jsonPath("$[0].type").value(testNotification.getType().toString()));
    }

    @Test
    void getNotificationById_shouldReturnNotification_whenExists() throws Exception {
        mockMvc.perform(get("/api/notifications/{id}", testNotification.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testNotification.getId()))
                .andExpect(jsonPath("$.shipmentId").value(testNotification.getShipmentId()))
                .andExpect(jsonPath("$.message").value(testNotification.getMessage()))
                .andExpect(jsonPath("$.type").value(testNotification.getType().toString()));
    }

    @Test
    void getNotificationById_shouldReturnNotFound_whenNotExists() throws Exception {
        mockMvc.perform(get("/api/notifications/{id}", "non-existent-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNotificationsByShipmentId_shouldReturnNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/shipment/{shipmentId}", testNotification.getShipmentId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testNotification.getId()))
                .andExpect(jsonPath("$[0].shipmentId").value(testNotification.getShipmentId()));
    }

    @Test
    void deleteNotification_shouldReturnNoContent_whenExists() throws Exception {
        mockMvc.perform(delete("/api/notifications/{id}", testNotification.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/{id}", testNotification.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNotification_shouldReturnNotFound_whenNotExists() throws Exception {
        mockMvc.perform(delete("/api/notifications/{id}", "non-existent-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllNotifications_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}