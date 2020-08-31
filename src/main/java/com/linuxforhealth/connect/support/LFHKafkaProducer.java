/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents an instance of a Kafka producer
 */
public class LFHKafkaProducer {
    
    private final Logger logger = LoggerFactory.getLogger(LFHKafkaProducer.class);
    private KafkaProducer producer = null;

    public void LFHKafkaProducer() {
    }

    /**
     * Start the KafkaProducer
     */
    protected void start(String brokers) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer(props);
    }

    /**
     * Send a message to Kafka
     */
    public void send(String topic, String key, String value) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, value);
            producer.send(record);
            producer.flush();
        } catch (Exception ex) {
            logger.error("Exception: "+ex);
        }   
    }

    /**
     * Stop the KafkaProducer
     */
    protected void close() throws Exception {
        if (producer != null) {
            producer.close();
            producer = null;
        }
    }
}
