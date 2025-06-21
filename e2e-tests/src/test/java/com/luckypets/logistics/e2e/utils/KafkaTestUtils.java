package com.luckypets.logistics.e2e.utils;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaTestUtils {

    public static KafkaConsumer<String, String> createConsumer(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new KafkaConsumer<>(props);
    }

    public static boolean waitForMessages(KafkaConsumer<String, String> consumer, String topic,
                                          int expectedCount, Duration timeout) {
        consumer.subscribe(Collections.singletonList(topic));

        long endTime = System.currentTimeMillis() + timeout.toMillis();
        int messageCount = 0;

        while (System.currentTimeMillis() < endTime && messageCount < expectedCount) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                messageCount++;
                System.out.println("Received message: " + record.value());

                if (messageCount >= expectedCount) {
                    return true;
                }
            }
        }

        return messageCount >= expectedCount;
    }
}