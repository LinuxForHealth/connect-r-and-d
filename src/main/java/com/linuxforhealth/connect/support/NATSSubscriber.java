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
    private Connection nc = null;
    private String topic = "lfh-remote-events";
    private String host;
    private String subject;

    public void NATSSubscriber() {
    }

    /**
     * Start the NATS subscriber.
     */
    public void start(String host, String subject, Options options, LFHKafkaProducer producer) {
        this.host = host;
        this.subject = subject;

        try {
            nc = Nats.connect(options);
            logger.info("nats-subscriber-"+host+"-"+subject+" listening on "+host+" and subject "+subject);

            Dispatcher d = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData(), StandardCharsets.UTF_8);
                logger.debug("nats-subscriber-"+host+"-"+subject+" received message: "+message);

                // Only store data received from other LFH instances
                if(!host.contains("localhost") && !host.contains("127.0.0.1") && !host.contains("nats-server")) {
                    producer.send(topic, null, message);
                    logger.debug("nats-subscriber-"+host+"-"+subject+" published message to topic: "+topic);
                }
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
    }
}
