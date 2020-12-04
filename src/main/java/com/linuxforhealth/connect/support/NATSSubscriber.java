/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that creates an instance of a NATS subscriber
 */
public class NATSSubscriber implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(NATSSubscriber.class);
    private Connection nc = null;
    private String topic = "lfh-remote-events";
    private String host;
    private String subject;
    private Options options;
    private LFHKafkaProducer producer;
    private Properties properties;

    public NATSSubscriber() {}

    public NATSSubscriber(String host, String subject, Options options, LFHKafkaProducer producer,
        Properties properties) {
        this.host = host;
        this.subject = subject;
        this.options = options;
        this.producer = producer;
        this.properties = properties;
    }

    /**
     * Start the NATS subscribers.
     */
    public void run() {
        int retries = Integer.parseInt(properties.getProperty("lfh.connect.messaging.retries"));
        int timeToWait = Integer.parseInt(properties.getProperty("lfh.connect.messaging.retry.interval.seconds")) * 1000;

        for (int i=0; i<retries; i++) {
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
                break;
            } catch (Exception ex) {
                logger.error("Retrying: "+ex);
                try {
                    if (i<retries-1) { Thread.sleep(timeToWait); }
                } catch (InterruptedException iex) {
                    logger.error("Interrupted Exception: "+iex);
                }
            }
        }
    }
}
