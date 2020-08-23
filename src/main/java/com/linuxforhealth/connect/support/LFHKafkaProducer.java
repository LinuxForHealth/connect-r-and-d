/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.Producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents an instance of a Kafka producer
 */
public class LFHKafkaProducer {
    
    private final Logger logger = LoggerFactory.getLogger(LFHKafkaProducer.class);
    private Producer<String, String> producer;

    public void LFHKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9094");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384); // 16KB buffer size
        props.put("linger.ms", 1);
        props.put("buffer.memory", 8388608);  // 8MB memory avail for buffering
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<String, String>(props);
    }

    /**
    * Send a message to Kafka.
    */
    public void send(String topic, String key, String value) {
        logger.info("LFHKafkaProducer publishing to topic: "+topic+" with value: "+value);
        try {
            ProducerRecord<String, String> record = new ProducerRecord<String, String>("lhf-remote-events", 0, System.currentTimeMillis(), "Camel", "temp");
            logger.info("record: "+record);
            producer.send(record);
        } catch (Exception ex) {
            //logger.error("Exception: "+ex);
            ex.printStackTrace();
        }   
    }

    /**
    * Stop the Kafka producer.
    */
    public void close() throws Exception {
        producer.close();
    }
}
