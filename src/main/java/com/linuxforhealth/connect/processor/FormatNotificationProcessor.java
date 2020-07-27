/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;

import java.util.ArrayList;

/**
 * Format the message for data storage notification 
 */
public class FormatNotificationProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
        msg.setDataStoreResult(exchange.getIn().getHeader(KafkaConstants.KAFKA_RECORDMETA, ArrayList.class));
        exchange.getIn().setBody(msg.toString());
    }
}
