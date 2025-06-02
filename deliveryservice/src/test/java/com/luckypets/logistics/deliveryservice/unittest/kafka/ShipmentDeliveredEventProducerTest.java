package com.luckypets.logistics.deliveryservice.unittest.kafka;

import com.luckypets.logistics.deliveryservice.kafka.ShipmentDeliveredEventProducer;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentDeliveredEventProducer Unit Tests")
class ShipmentDeliveredEventProducerTest {

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @InjectMocks
    private ShipmentDeliveredEventProducer producer;

    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String TOPIC = "shipment-delivered";

    @Test
    @DisplayName("Should send ShipmentDeliveredEvent to Kafka")
    void sendShipmentDeliveredEvent_shouldSendEventToKafka() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                SHIPMENT_ID,
                "Munich",
                "Berlin",
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );

        // Mock the KafkaTemplate send method to return a CompletableFuture
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        RecordMetadata recordMetadata = new RecordMetadata(topicPartition, 0, 0, 0, 0, 0);
        ProducerRecord<String, ShipmentDeliveredEvent> producerRecord = new ProducerRecord<>(TOPIC, SHIPMENT_ID, event);
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(producerRecord, recordMetadata);
        
        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.complete(sendResult);
        
        when(kafkaTemplate.send(eq(TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class))).thenReturn(future);

        // When
        producer.sendShipmentDeliveredEvent(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals(TOPIC, topicCaptor.getValue());
        assertEquals(SHIPMENT_ID, keyCaptor.getValue());
        assertEquals(event, eventCaptor.getValue());
    }

    @Test
    @DisplayName("Should handle successful send result")
    void sendShipmentDeliveredEvent_shouldHandleSuccessfulSendResult() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                SHIPMENT_ID,
                "Munich",
                "Berlin",
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );

        // Mock the KafkaTemplate send method to return a CompletableFuture
        TopicPartition topicPartition = new TopicPartition(TOPIC, 1);
        RecordMetadata recordMetadata = new RecordMetadata(topicPartition, 0, 123, 0, 0, 0);
        ProducerRecord<String, ShipmentDeliveredEvent> producerRecord = new ProducerRecord<>(TOPIC, SHIPMENT_ID, event);
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(producerRecord, recordMetadata);
        
        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.complete(sendResult);
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        producer.sendShipmentDeliveredEvent(event);

        // Then
        // Verification is implicit - no exceptions should be thrown
        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle failed send result")
    void sendShipmentDeliveredEvent_shouldHandleFailedSendResult() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                SHIPMENT_ID,
                "Munich",
                "Berlin",
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );

        // Mock the KafkaTemplate send method to return a CompletableFuture that completes exceptionally
        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        producer.sendShipmentDeliveredEvent(event);

        // Then
        // Verification is implicit - no exceptions should be thrown
        verify(kafkaTemplate).send(any(), any(), any());
    }
}