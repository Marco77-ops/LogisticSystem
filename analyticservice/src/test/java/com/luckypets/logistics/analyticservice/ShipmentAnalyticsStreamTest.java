package com.luckypets.logistics.analyticservice;

import com.luckypets.logistics.shared.events.ShipmentAnalyticsEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.analyticservice.service.AnalyticsService;
import com.luckypets.logistics.analyticservice.service.AnalyticsServiceImpl;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;


public class ShipmentAnalyticsStreamTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, ShipmentDeliveredEvent> inputTopic;
    private TestOutputTopic<String, ShipmentAnalyticsEvent> outputTopic;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        // Stream-Topologie erstellen
        StreamsBuilder builder = new StreamsBuilder();
        analyticsService = new AnalyticsServiceImpl("shipment-delivered", "shipment-analytics");
        analyticsService.buildAnalyticsStream(builder);
        Topology topology = builder.build();

        // Kafka Streams Eigenschaften
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        // Test Driver erstellen
        testDriver = new TopologyTestDriver(topology, props);

        // Serdes für Input und Output
        JsonSerde<ShipmentDeliveredEvent> deliveredSerde = new JsonSerde<>(ShipmentDeliveredEvent.class);
        JsonSerde<ShipmentAnalyticsEvent> analyticsSerde = new JsonSerde<>(ShipmentAnalyticsEvent.class);

        // Test-Topics erstellen
        inputTopic = testDriver.createInputTopic(
                "shipment-delivered",
                Serdes.String().serializer(),
                deliveredSerde.serializer()
        );

        outputTopic = testDriver.createOutputTopic(
                "shipment-analytics",
                Serdes.String().deserializer(),
                analyticsSerde.deserializer()
        );
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldAggregateShipmentsPerLocation() {
        // Given
        Instant baseTime = Instant.parse("2024-01-01T10:00:00Z");
        String location = "Berlin";

        ShipmentDeliveredEvent event1 = new ShipmentDeliveredEvent(
                "shipment-1",
                "Berlin",
                location,
                LocalDateTime.ofInstant(baseTime, ZoneOffset.UTC),
                "corr-1"
        );

        ShipmentDeliveredEvent event2 = new ShipmentDeliveredEvent(
                "shipment-2",
                "Berlin",
                location,
                LocalDateTime.ofInstant(baseTime.plusSeconds(1000), ZoneOffset.UTC),
                "corr-2"
        );

        // When
        inputTopic.pipeInput(event1.getShipmentId(), event1, baseTime.toEpochMilli());
        inputTopic.pipeInput(event2.getShipmentId(), event2, baseTime.plusSeconds(1000).toEpochMilli());

        // Then
        List<TestRecord<String, ShipmentAnalyticsEvent>> outputRecords = outputTopic.readRecordsToList();
        assertFalse(outputRecords.isEmpty());

        TestRecord<String, ShipmentAnalyticsEvent> lastRecord = outputRecords.get(outputRecords.size() - 1);
        ShipmentAnalyticsEvent analyticsEvent = lastRecord.getValue();

        assertEquals(location, analyticsEvent.getLocation());
        assertEquals(2L, analyticsEvent.getCount());
        assertNotNull(analyticsEvent.getWindowStart());
    }

    @Test
    void shouldCreateSeparateWindowsForDifferentLocations() {
        // Given
        Instant baseTime = Instant.parse("2024-01-01T10:00:00Z");

        ShipmentDeliveredEvent berlinEvent = new ShipmentDeliveredEvent(
                "shipment-1",
                "Berlin",
                "Berlin",
                LocalDateTime.ofInstant(baseTime, ZoneOffset.UTC),
                "corr-1"
        );

        ShipmentDeliveredEvent hamburgEvent = new ShipmentDeliveredEvent(
                "shipment-2",
                "Hamburg",
                "Hamburg",
                LocalDateTime.ofInstant(baseTime.plusSeconds(1000), ZoneOffset.UTC),
                "corr-2"
        );

        // When
        inputTopic.pipeInput(berlinEvent.getShipmentId(), berlinEvent, baseTime.toEpochMilli());
        inputTopic.pipeInput(hamburgEvent.getShipmentId(), hamburgEvent, baseTime.plusSeconds(1000).toEpochMilli());

        // Then
        List<TestRecord<String, ShipmentAnalyticsEvent>> outputRecords = outputTopic.readRecordsToList();
        assertTrue(outputRecords.size() >= 2);

        // Überprüfe, dass wir separate Fenster für Berlin und Hamburg haben
        boolean hasBerlin = false;
        boolean hasHamburg = false;

        for (TestRecord<String, ShipmentAnalyticsEvent> record : outputRecords) {
            ShipmentAnalyticsEvent event = record.getValue();
            if (event.getLocation().equals("Berlin")) {
                hasBerlin = true;
                assertEquals(1L, event.getCount());
            }
            if (event.getLocation().equals("Hamburg")) {
                hasHamburg = true;
                assertEquals(1L, event.getCount());
            }
        }

        assertTrue(hasBerlin, "Es sollte ein Analytics-Event für Berlin geben");
        assertTrue(hasHamburg, "Es sollte ein Analytics-Event für Hamburg geben");
    }

    @Test
    void shouldAggregateShipmentsInDifferentHourWindowsSeparately() {
        // Given
        Instant time1 = Instant.parse("2024-01-01T10:15:00Z");
        Instant time2 = Instant.parse("2024-01-01T11:05:00Z"); // Nächste Stunde
        String location = "Berlin";

        ShipmentDeliveredEvent early = new ShipmentDeliveredEvent(
                "s1",
                location,
                location,
                LocalDateTime.ofInstant(time1, ZoneOffset.UTC),
                "c1"
        );
        ShipmentDeliveredEvent late = new ShipmentDeliveredEvent(
                "s2",
                location,
                location,
                LocalDateTime.ofInstant(time2, ZoneOffset.UTC),
                "c2"
        );

        // When
        inputTopic.pipeInput(early.getShipmentId(), early, time1.toEpochMilli());
        inputTopic.pipeInput(late.getShipmentId(), late, time2.toEpochMilli());

        // Then
        List<TestRecord<String, ShipmentAnalyticsEvent>> records = outputTopic.readRecordsToList();

        // Suche nach Events in den verschiedenen Zeitfenstern
        long count10 = records.stream()
                .filter(r -> r.getValue().getWindowStart().equals(
                        Instant.parse("2024-01-01T10:00:00Z")))
                .map(r -> r.getValue().getCount())
                .findFirst()
                .orElse(0L);

        long count11 = records.stream()
                .filter(r -> r.getValue().getWindowStart().equals(
                        Instant.parse("2024-01-01T11:00:00Z")))
                .map(r -> r.getValue().getCount())
                .findFirst()
                .orElse(0L);

        // Überprüfungen
        assertEquals(1L, count10, "Erstes Stunden-Fenster sollte eine Lieferung enthalten");
        assertEquals(1L, count11, "Zweites Stunden-Fenster sollte eine Lieferung enthalten");
        assertTrue(records.size() >= 2, "Es sollten mindestens zwei separate Fenster existieren");
    }
}
