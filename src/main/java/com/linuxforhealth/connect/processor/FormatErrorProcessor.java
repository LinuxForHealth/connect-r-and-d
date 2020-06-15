/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.processor.LinuxForHealthProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

/**
 * Format the message for error notification 
 */
public class FormatErrorProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        final Throwable ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        JSONObject top = new JSONObject();
        JSONObject meta = new JSONObject();
        JSONObject error = new JSONObject();

        setCommonMeta(exchange, meta);
        meta.put("status", "error");
        error.put("message", ex.getMessage());
        top.put("meta", meta);
        top.put("data", error);

        exchange.getIn().setBody(top.toString());
    }
}
