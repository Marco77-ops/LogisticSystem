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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // Korrekter Import für ChronoUnit
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within; // Import für within
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Ermöglicht die Verwendung von @Mock Annotationen
class ShipmentScannedListenerTest {

    @Mock
    private ShipmentRepository repository;

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    private ShipmentScannedListener listener;

    private final String testDeliveredTopic = "shipment-delivered-test-topic";

    @BeforeEach
    void setup() {
        // Die Mocks werden durch @ExtendWith(MockitoExtension.class) und @Mock initialisiert
        listener = new ShipmentScannedListener(repository, kafkaTemplate, testDeliveredTopic);
    }

    @SuppressWarnings("unchecked") // Diese ist hier für SendResult noch nötig, da der Typ des Futures nicht vollständig spezifiziert ist.
    private CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> mockKafkaSend() {
        SendResult<String, ShipmentDeliveredEvent> sendResultMock = mock(SendResult.class);
        RecordMetadata metadataMock = mock(RecordMetadata.class);
        when(metadataMock.topic()).thenReturn(testDeliveredTopic);
        when(metadataMock.partition()).thenReturn(0);
        when(metadataMock.offset()).thenReturn(1L); // Beispiel Offset
        when(sendResultMock.getRecordMetadata()).thenReturn(metadataMock);
        return CompletableFuture.completedFuture(sendResultMock);
    }

    @Test
    @DisplayName("Neues Shipment wird gespeichert und DeliveredEvent gesendet, wenn Ziel erreicht")
    void shouldSaveNewShipmentEntityAndSendDeliveredEventWhenDestinationIsReached() {
        LocalDateTime scannedAt = LocalDateTime.now();
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S123", "Berlin", scannedAt, "Berlin", "test-correlation-id-123"
        );

        when(repository.findById("S123")).thenReturn(Optional.empty());
        when(kafkaTemplate.send(anyString(), anyString(), any(ShipmentDeliveredEvent.class)))
                .thenReturn(mockKafkaSend());

        listener.onShipmentScanned(event);

        ArgumentCaptor<ShipmentEntity> entityCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(entityCaptor.capture());

        ShipmentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getShipmentId()).isEqualTo("S123");
        assertThat(savedEntity.getLastLocation()).isEqualTo("Berlin");
        assertThat(savedEntity.getDestination()).isEqualTo("Berlin");
        assertThat(savedEntity.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(savedEntity.getDeliveredAt()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getLastScannedAt()).isEqualTo(scannedAt);

        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(kafkaTemplate).send(eq(testDeliveredTopic), eq("S123"), eventCaptor.capture());

        ShipmentDeliveredEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getShipmentId()).isEqualTo("S123");
        assertThat(capturedEvent.getLocation()).isEqualTo("Berlin");
        assertThat(capturedEvent.getDestination()).isEqualTo("Berlin");
        assertThat(capturedEvent.getCorrelationId()).isEqualTo("test-correlation-id-123");
        assertThat(capturedEvent.getTimestamp()).isNotNull(); // Geändert von getDeliveredAt()
        // Optionale genauere Zeitprüfung
        assertThat(capturedEvent.getTimestamp()).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS)); // Geändert von getDeliveredAt()
    }

    @Test
    @DisplayName("Bestehendes Shipment wird aktualisiert, wenn Ziel nicht erreicht")
    void shouldUpdateExistingShipmentWhenDestinationNotReached() {
        LocalDateTime scannedAt = LocalDateTime.now();
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S999", "Leipzig", scannedAt, "Berlin", "test-correlation-id-456"
        );

        ShipmentEntity existingEntity = new ShipmentEntity();
        existingEntity.setShipmentId("S999");
        existingEntity.setDestination("Berlin");
        existingEntity.setStatus(ShipmentStatus.IN_TRANSIT);
        existingEntity.setLastLocation("Dresden");
        existingEntity.setCreatedAt(LocalDateTime.now().minusDays(1)); // Annahme: wurde vorher erstellt

        when(repository.findById("S999")).thenReturn(Optional.of(existingEntity));

        listener.onShipmentScanned(event);

        ArgumentCaptor<ShipmentEntity> entityCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(entityCaptor.capture());

        ShipmentEntity updatedEntity = entityCaptor.getValue();
        assertThat(updatedEntity.getShipmentId()).isEqualTo("S999");
        assertThat(updatedEntity.getLastLocation()).isEqualTo("Leipzig");
        assertThat(updatedEntity.getDestination()).isEqualTo("Berlin");
        assertThat(updatedEntity.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(updatedEntity.getDeliveredAt()).isNull();
        assertThat(updatedEntity.getLastScannedAt()).isEqualTo(scannedAt);
        assertThat(updatedEntity.getCreatedAt()).isEqualTo(existingEntity.getCreatedAt()); // CreatedAt sollte unverändert bleiben

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(ShipmentDeliveredEvent.class));
    }

    @Test
    @DisplayName("Bestehendes Shipment wird aktualisiert und DeliveredEvent gesendet, wenn Ziel erreicht")
    void shouldUpdateExistingShipmentAndSendDeliveredEventWhenDestinationIsReached() {
        LocalDateTime scannedAt = LocalDateTime.now();
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S789", "München", scannedAt, "München", "test-correlation-id-789"
        );

        ShipmentEntity existingEntity = new ShipmentEntity();
        existingEntity.setShipmentId("S789");
        existingEntity.setDestination("München");
        existingEntity.setStatus(ShipmentStatus.IN_TRANSIT);
        existingEntity.setLastLocation("Nürnberg");
        existingEntity.setCreatedAt(LocalDateTime.now().minusHours(5)); // Annahme: wurde vorher erstellt

        when(repository.findById("S789")).thenReturn(Optional.of(existingEntity));
        when(kafkaTemplate.send(anyString(), anyString(), any(ShipmentDeliveredEvent.class)))
                .thenReturn(mockKafkaSend()); // Mock für erfolgreiches Senden

        listener.onShipmentScanned(event);

        ArgumentCaptor<ShipmentEntity> entityCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(entityCaptor.capture());

        ShipmentEntity updatedEntity = entityCaptor.getValue();
        assertThat(updatedEntity.getShipmentId()).isEqualTo("S789");
        assertThat(updatedEntity.getLastLocation()).isEqualTo("München");
        assertThat(updatedEntity.getDestination()).isEqualTo("München");
        assertThat(updatedEntity.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(updatedEntity.getDeliveredAt()).isNotNull();
        assertThat(updatedEntity.getLastScannedAt()).isEqualTo(scannedAt);
        assertThat(updatedEntity.getCreatedAt()).isEqualTo(existingEntity.getCreatedAt()); // CreatedAt sollte unverändert bleiben

        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(kafkaTemplate).send(eq(testDeliveredTopic), eq("S789"), eventCaptor.capture());

        ShipmentDeliveredEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getShipmentId()).isEqualTo("S789");
        assertThat(capturedEvent.getLocation()).isEqualTo("München");
        assertThat(capturedEvent.getDestination()).isEqualTo("München");
        assertThat(capturedEvent.getCorrelationId()).isEqualTo("test-correlation-id-789");
        assertThat(capturedEvent.getTimestamp()).isNotNull(); // Geändert von getDeliveredAt()
        assertThat(capturedEvent.getTimestamp()).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS)); // Geändert von getDeliveredAt()
    }

    // Beibehalten als spezifischer Testfall, obwohl Teile von anderen Tests abgedeckt werden.
    @Test
    @DisplayName("Zwischenstation: Shipment bleibt 'In Transit' ohne Event-Versand")
    void shouldNotSendDeliveredEventIfNotAtDestinationAndIsExistingShipment() {
        LocalDateTime scannedAt = LocalDateTime.now();
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "S456", "Frankfurt", scannedAt, "Berlin", "test-correlation-id-intermediate"
        );

        ShipmentEntity existingEntity = new ShipmentEntity();
        existingEntity.setShipmentId("S456");
        existingEntity.setDestination("Berlin");
        existingEntity.setStatus(ShipmentStatus.IN_TRANSIT);
        existingEntity.setLastLocation("Erfurt"); // Vorheriger Ort
        existingEntity.setCreatedAt(LocalDateTime.now().minusDays(2));


        when(repository.findById("S456")).thenReturn(Optional.of(existingEntity));

        listener.onShipmentScanned(event);

        ArgumentCaptor<ShipmentEntity> entityCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(entityCaptor.capture());
        ShipmentEntity savedEntity = entityCaptor.getValue();

        assertThat(savedEntity.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(savedEntity.getLastLocation()).isEqualTo("Frankfurt");
        assertThat(savedEntity.getDeliveredAt()).isNull();
        assertThat(savedEntity.getLastScannedAt()).isEqualTo(scannedAt);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}