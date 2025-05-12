package com.luckypets.logistics.deliveryservice.listener;

import com.luckypets.logistics.deliveryservice.persistence.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShipmentScannedListenerTest {

    private ShipmentRepository repository;

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    private ShipmentScannedListener listener;

    @BeforeEach
    void setup() {
        repository = mock(ShipmentRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        listener = new ShipmentScannedListener(repository, kafkaTemplate, "shipment-delivered");
    }

    @Test
    @DisplayName("Neues Shipment wird gespeichert, wenn nicht vorhanden")
    void shouldSaveNewShipmentEntity() {
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S123", "Berlin", LocalDateTime.now(), "Berlin", "test-correlation-id"
        );

        when(repository.findById("S123")).thenReturn(Optional.empty());

        SendResult<String, ShipmentDeliveredEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.partition()).thenReturn(1);
        when(metadata.offset()).thenReturn(123L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        listener.onShipmentScanned(event);

        ArgumentCaptor<ShipmentEntity> captor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(captor.capture());

        ShipmentEntity saved = captor.getValue();
        assertThat(saved.getShipmentId()).isEqualTo("S123");
        assertThat(saved.getLastLocation()).isEqualTo("Berlin");
        assertThat(saved.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(saved.getDeliveredAt()).isNotNull();

        verify(kafkaTemplate).send(eq("shipment-delivered"), eq("S123"), any());
    }

    @Test
    @DisplayName("Bestehendes Shipment wird aktualisiert")
    void shouldUpdateExistingShipment() {
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S999", "Leipzig", LocalDateTime.now(), "Berlin", "test-correlation-id"
        );

        ShipmentEntity existing = new ShipmentEntity();
        existing.setShipmentId("S999");
        existing.setDestination("Berlin");

        when(repository.findById("S999")).thenReturn(Optional.of(existing));

        listener.onShipmentScanned(event);

        verify(repository).save(argThat(updated ->
                updated.getLastLocation().equals("Leipzig") &&
                        updated.getStatus() == ShipmentStatus.IN_TRANSIT
        ));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Zwischenstation: Shipment bleibt 'In Transit' ohne Event")
    void shouldNotSendDeliveredEventIfNotAtDestination() {
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S456", "Frankfurt", LocalDateTime.now(), "Berlin", "test-correlation-id"
        );

        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId("S456");
        entity.setDestination("Berlin");

        when(repository.findById("S456")).thenReturn(Optional.of(entity));

        listener.onShipmentScanned(event);

        verify(repository).save(argThat(updated ->
                updated.getStatus() == ShipmentStatus.IN_TRANSIT &&
                        updated.getLastLocation().equals("Frankfurt")
        ));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
