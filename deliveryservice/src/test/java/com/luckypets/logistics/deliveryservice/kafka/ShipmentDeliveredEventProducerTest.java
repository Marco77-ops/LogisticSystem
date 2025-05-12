package com.luckypets.logistics.deliveryservice.kafka;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ShipmentDeliveredEventProducerTest {

    @Autowired
    private ShipmentDeliveredEventProducer producer;

    @Test
    void testShipmentDeliveredEventIsSentToKafka() {
        // Arrange
        String topic = "shipment-delivered";
        String shipmentId = "manual-123";
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
        Objects.requireNonNull(shipmentId, "shipmentId darf nicht null sein"),
        "Hamburg",  // unterschiedliche Werte für bessere Testbarkeit
        "Berlin",
        Instant.now(),  // Instant statt LocalDateTime
        "test-correlation-id"
);

        // Act – sende das Event
        producer.sendShipmentDeliveredEvent(event);

        // Setup Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "manual-test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ShipmentDeliveredEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        try (KafkaConsumer<String, ShipmentDeliveredEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));

            // Poll auf das Event
            ConsumerRecords<String, ShipmentDeliveredEvent> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThan(0);

            ConsumerRecord<String, ShipmentDeliveredEvent> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(shipmentId);
            assertThat(record.value().getLocation()).isEqualTo("Berlin");

            System.out.println("✅ Event empfangen: " + record.value());
        }
    }
}