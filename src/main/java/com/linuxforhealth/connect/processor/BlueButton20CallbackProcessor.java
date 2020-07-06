/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set up Blue Button 2.0 request for an authorization code
 */
public class BlueButton20CallbackProcessor extends LinuxForHealthProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(BlueButton20CallbackProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String clientId = uriBuilder.getBlueButton20ClientId();
        String clientSecret = uriBuilder.getBlueButton20ClientSecret();

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
