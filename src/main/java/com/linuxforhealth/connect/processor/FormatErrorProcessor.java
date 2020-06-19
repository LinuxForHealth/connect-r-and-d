/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Format the message for error notification 
 */
public class FormatErrorProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        final Throwable ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
        msg.setError(ex.getMessage());
        exchange.getIn().setBody(msg.toString());
    }
}
