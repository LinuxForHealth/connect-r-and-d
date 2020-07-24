/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.codec.binary.Base64;

/**
 * Set up Blue Button 2.0 request for an authorization code
 */
public class BlueButton20CallbackProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        String clientId = SimpleBuilder
                .simple("${properties:lfh.connect.bluebutton_20_rest.clientId}")
                .evaluate(exchange, String.class);

        String clientSecret = SimpleBuilder
                .simple("${properties:lfh.connect.bluebutton_20_rest.clientSecret}")
                .evaluate(exchange, String.class);

        // Setting up call to Blue Button 2.0 to exchange the code for a token
        String code  = exchange.getIn().getHeader("code", String.class);
        String body = "code="+code+"&grant_type=authorization_code";
        String auth = clientId+":"+clientSecret;
        String authHeader = "Basic "+Base64.encodeBase64String(auth.getBytes());
        exchange.getOut().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getOut().setHeader("Authorization", authHeader);
        exchange.getOut().setHeader("Content-Type", "application/x-www-form-urlencoded");
        exchange.getOut().setHeader("Content-Length", body.length());
        exchange.getOut().setBody(body);
    }
}
