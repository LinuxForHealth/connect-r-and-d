/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.main.Main;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 * Class that creates and manages LinuxForHealth service instances
 */
public class LFHServiceManager {

    private final static Logger logger = LoggerFactory.getLogger(LFHServiceManager.class);
    private static LFHKafkaProducer producer = null;
    private static LFHKafkaConsumer consumer = null;

    public void LFHServiceManager() { }

   /**
    * Start the services needed for LinuxForHealth:
    *   1. NATS subscribers defined in application.properties.
    *   2. Kafka producer needed to store remote LFH messages to the local Kafka.
    *   3. Kafka consumer needed to retrieve data from a (topic, partition, offset)
    */
    public static void startServices(Properties properties, Main camelMain) {
        String[] hosts = properties.getProperty("lfh.connect.messaging.subscribe.hosts").split(",");
        String subject = properties.getProperty("lfh.connect.messaging.subscribe.subject");
        String brokers = properties.getProperty("lfh.connect.datastore.brokers");
        producer = new LFHKafkaProducer();
        consumer = new LFHKafkaConsumer();
        camelMain.bind("LFHKafkaConsumer", consumer);

        try {
            consumer.start(brokers);
            producer.start(brokers);
            for (String host: hosts) {
                NATSSubscriber subscriber = new NATSSubscriber(host, subject,
                    createOptions(host, true), producer, properties);
                new Thread(subscriber).start();
            }
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getMessage());
        }
    }

   /**
    * Stop the LinuxForHealth services.
    */
    public static void stopServices() {
        try {
            if (producer != null) {
                producer.close();
                producer = null;
            }

            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getMessage());
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
