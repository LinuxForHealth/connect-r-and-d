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

    public void NATSSubscriber() { }

    public void start(String host, String subject, Options options) {
        try {
            nc = Nats.connect(options);
            logger.info("Listening on "+host+" and subject "+subject);

            Dispatcher d = nc.createDispatcher((msg) -> {
                logger.info("nats-subscriber-"+host+"-"+subject+" received message: "+
                    new String(msg.getData(), StandardCharsets.UTF_8));
            });

            d.subscribe(subject);
        } catch (Exception ex) {
            logger.error("Exception: "+ex.getMessage());
        }
    }

    public void stop() {
        try {
            nc.flush(Duration.ZERO);
            nc.close();
        } catch (Exception ex) {
            logger.error("Exception: "+ex.getMessage());
        }
    }
}
