/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents an instance of a NATS subscriber
 */
public class NATSSubscriber {
    
    private final Logger logger = LoggerFactory.getLogger(NATSSubscriber.class);
    private Connection nc;
    private String topic = "lhf-remote-events";
    private String host;
    private String subject;
    private LFHKafkaProducer producer;

    public void NATSSubscriber() {
    }

    /**
    * Start the NATS subscriber.
    */
    public void start(String host, String subject, Options options, LFHKafkaProducer producer) {
        this.host = host;
        this.subject = subject;
        this.producer = producer;

        try {
            // Connect to the NATS server
            nc = Nats.connect(options);
            logger.info("nats-subscriber-"+host+"-"+subject+" listening on "+host+" and subject "+subject);

            Dispatcher d = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData(), StandardCharsets.UTF_8);
                logger.info("nats-subscriber-"+host+"-"+subject+" received message: "+message);

                // Only store data received from other LFH instances
//                if(!host.contains("localhost") && !host.contains("127.0.0.1")) {
                    logger.info("nats-subscriber-"+host+"-"+subject+" publishing to topic: "+topic);
                    producer.send(topic, "Camel", message);
                    logger.info("nats-subscriber-"+host+"-"+subject+" after publishing to topic: "+topic);
                //}

            });

            d.subscribe(subject);
        } catch (Exception ex) {
            logger.error("Exception: "+ex.getMessage());
        }
    }

    /**
    * Stop the NATS subscriber.
    */
    public void close() throws Exception {
        logger.info("nats-subscriber-"+host+"-"+subject+" close() called");
        if (nc != null) {
            nc.flush(Duration.ZERO);
            nc.close();
        }
        if (producer != null) producer.close();
    }
}
