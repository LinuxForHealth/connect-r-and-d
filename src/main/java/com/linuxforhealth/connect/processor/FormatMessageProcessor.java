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
 * Format the message for data storage
 */
public class FormatMessageProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        JSONObject top = new JSONObject();
        JSONObject meta = new JSONObject();

        setCommonMeta(exchange, meta);
        top.put("meta", meta);
        top.put("data", exchange.getIn().getBody());

        exchange.getIn().setBody(top.toString());
    }
}
