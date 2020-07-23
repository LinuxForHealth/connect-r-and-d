/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Format the message for data storage
 */
public class FormatMessageProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
        msg.setData(exchange.getIn().getBody());
        exchange.getIn().setBody(msg.toString());
    }
}
