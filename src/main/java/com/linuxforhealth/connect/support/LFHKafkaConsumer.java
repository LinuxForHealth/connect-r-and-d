/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents an instance of a Kafka consumer, specifically designed 
 * to retrieve data from Kafka at a specific (topic, partition, offset).
 */
public class LFHKafkaConsumer {
    
    private final Logger logger = LoggerFactory.getLogger(LFHKafkaConsumer.class);
    private KafkaConsumer consumer = null;

    public void LFHKafkaConsumer() { }

    /**
     * Start the Kafka consumer
     */
    public void start() {
        final Properties lfhProps = LFHUtils.loadProperties();
        final String brokers = lfhProps.getProperty("lfh.connect.datastore.brokers");
        final Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "LFHKafkaConsumerGroup");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer(props);
     }

    /**
     * Get a message from Kafka
     */
    public String get(String topic, int partition, long offset) {
        String value = null;

        // start on first execution
        if (consumer == null) start();

        logger.debug("input: partition={} offset={} topic={}", partition, offset, topic);
        TopicPartition topicPartition = new TopicPartition(topic, partition);

        try {
            consumer.assign(Arrays.asList(topicPartition));  // subscribe
            consumer.seek(topicPartition, offset);
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            logger.debug("received {} records", records.count());
            for (ConsumerRecord<String, String> record : records) {
                // should only be 1 record
                value = record.value();
            }
        } catch (Exception ex) {
            logger.error("Exception: "+ex);
        } finally {
            consumer.assign(Arrays.asList());  // unsubscribe
        }

        return value;   
    }

    /**
     * Stop the KafkaConsumer
     */
    protected void close() throws Exception {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }
}
