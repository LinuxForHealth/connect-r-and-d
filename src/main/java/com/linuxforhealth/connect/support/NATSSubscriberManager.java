/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that creates NATS subscriber instances
 */
public class NATSSubscriberManager {

    private final static Logger logger = LoggerFactory.getLogger(NATSSubscriberManager.class);

    public void NATSSubscriberManager() { }

    /**
    * Start the NATS subscribers defined in application.properties.
    */
    public static void startSubscribers(Properties properties) {
        String[] hosts = properties.getProperty("lfh.connect.messaging.subscribe.hosts").split(",");
        String subject = properties.getProperty("lfh.connect.messaging.subscribe.subject");

        for (String host: hosts) {
            LFHKafkaProducer producer = new LFHKafkaProducer();
            NATSSubscriber sub = new NATSSubscriber();
            try {
                sub.start(host, subject, createOptions(host, true), producer);
            } catch(Exception ex) {
                logger.error("Exception1: "+ex.toString());
                logger.error("Exception2: " + ex.getMessage());
            }
        }
    }

    /**
    * Set up the NATS connection options for subscribers.
    */
    public static Options createOptions(String server, boolean allowReconnect) throws Exception {
        Options.Builder builder = new Options.Builder()
                    .server(server)
                    .connectionTimeout(Duration.ofSeconds(5))
                    .pingInterval(Duration.ofSeconds(10))
                    .reconnectWait(Duration.ofSeconds(1))
                    .errorListener(new ErrorListener(){
                        public void exceptionOccurred(Connection conn, Exception ex) {
                            logger.error("Exception " + ex.getMessage());
                        }

                        public void errorOccurred(Connection conn, String type) {
                            logger.error("Error " + type);
                        }
                        
                        public void slowConsumerDetected(Connection conn, Consumer consumer) {
                            logger.error("Slow consumer");
                        }
                    })
                    .connectionListener(new ConnectionListener(){
                        public void connectionEvent(Connection conn, Events type) {
                            logger.info("Status change "+type);
                        }
                    });

        if (!allowReconnect) {
            builder = builder.noReconnect();
        } else {
            builder = builder.maxReconnects(-1);
        }

        return builder.build();
    }
}
