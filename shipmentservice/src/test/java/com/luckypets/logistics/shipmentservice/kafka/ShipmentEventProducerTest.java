package com.luckypets.logistics.shipmentservice.kafka;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentEventProducerTest {

    @Mock
    private KafkaTemplate<String, ShipmentCreatedEvent> mockKafkaTemplate;

    @InjectMocks
    private ShipmentEventProducer shipmentEventProducer;

    private ShipmentCreatedEvent testEvent;
    private String testShipmentId;
    private String testCorrelationId;

    @BeforeEach
    void setUp() {
        testShipmentId = UUID.randomUUID().toString();
        testCorrelationId = UUID.randomUUID().toString();
        testEvent = new ShipmentCreatedEvent(
                testShipmentId,
                "Test Destination",
                LocalDateTime.now(),
                testCorrelationId
        );
    }

    @Test
    @DisplayName("sendShipmentCreatedEvent sendet Event erfolgreich an Kafka")
    void sendShipmentCreatedEvent_sendsEventSuccessfully() {
        // Arrange
        CompletableFuture<SendResult<String, ShipmentCreatedEvent>> future = new CompletableFuture<>();
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("shipment-created", 0), 0, 0, System.currentTimeMillis(), (long) 0, 0, 0);
        SendResult<String, ShipmentCreatedEvent> sendResult = new SendResult<>(
                new ProducerRecord<>("shipment-created", testShipmentId, testEvent),
                recordMetadata
        );
        future.complete(sendResult);

        when(mockKafkaTemplate.send(any(String.class), any(String.class), any(ShipmentCreatedEvent.class)))
                .thenReturn(future);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ShipmentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentCreatedEvent.class);

        // Act
        shipmentEventProducer.sendShipmentCreatedEvent(testEvent);

        // Assert
        verify(mockKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertEquals("shipment-created", topicCaptor.getValue());
        assertEquals(testShipmentId, keyCaptor.getValue());
        assertEquals(testEvent, eventCaptor.getValue());
    }

    @Test
    @DisplayName("sendShipmentCreatedEvent behandelt Kafka Sendefehler korrekt")
    void sendShipmentCreatedEvent_handlesSendFailure() {
        // Arrange
        CompletableFuture<SendResult<String, ShipmentCreatedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test Kafka send failure")); // Simuliere einen Fehler

        when(mockKafkaTemplate.send(any(String.class), any(String.class), any(ShipmentCreatedEvent.class)))
                .thenReturn(future);

        // Act
        shipmentEventProducer.sendShipmentCreatedEvent(testEvent);

        // Assert
        // Verifiziere, dass die send Methode aufgerufen wurde.
        // Die Fehlerbehandlung innerhalb von sendShipmentCreatedEvent (Loggen der Exception) wird hier nicht direkt überprüft,
        // aber wir stellen sicher, dass der Aufruf an Kafka erfolgt und die Exception innerhalb des Producers gehandhabt wird,
        // ohne den Test zum Absturz zu bringen.
        verify(mockKafkaTemplate).send(any(String.class), any(String.class), any(ShipmentCreatedEvent.class));
        // Man könnte hier mit einem gemockten Logger prüfen, ob der Fehler geloggt wurde,
        // aber das macht den Test komplexer. Wichtig ist, dass der Kontrollfluss im Fehlerfall
        // wie erwartet ist und keine Exception aus der Methode `sendShipmentCreatedEvent` weitergeworfen wird.
    }
}