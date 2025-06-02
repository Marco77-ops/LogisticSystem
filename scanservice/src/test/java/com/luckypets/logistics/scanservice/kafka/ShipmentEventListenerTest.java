package com.luckypets.logistics.scanservice.kafka;

import com.luckypets.logistics.scanservice.repository.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShipmentEventListenerTest {

    private ShipmentRepository repository;
    private ShipmentEventListener listener;

    @BeforeEach
    void setup() {
        repository = mock(ShipmentRepository.class);
        listener = new ShipmentEventListener(repository);
    }

    @Test
    @DisplayName("Listener speichert ShipmentCreatedEvent in DB")
    void handleShipmentCreatedEvent_savesToRepository() {
        // Given
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "shipment-1",
                "Berlin",
                LocalDateTime.of(2025, 5, 5, 10, 0),
                "test-correlation-id"
        );

        // When
        listener.handleShipmentCreatedEvent(event);

        // Then
        ArgumentCaptor<ShipmentEntity> captor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository, times(1)).save(captor.capture());

        ShipmentEntity saved = captor.getValue();
        assertThat(saved.getShipmentId()).isEqualTo("shipment-1");
        assertThat(saved.getDestination()).isEqualTo("Berlin");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 5, 5, 10, 0));
    }
}
