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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set up the Blue Button 2.0 API query
 */
public class BlueButton20RequestProcessor extends LinuxForHealthProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(BlueButton20RequestProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String cmsBaseURL = uriBuilder.getBlueButton20CmsBaseUri();
        
        // Set up Blue Button 2.0 query
        String authorizationHdr =  exchange.getIn().getHeader("Authorization", String.class);
        String httpMethod = exchange.getIn().getHeader("CamelHttpMethod", String.class);
        String resource = exchange.getIn().getHeader("resource", String.class);
        String query = exchange.getIn().getHeader("CamelHttpQuery", String.class);
        String location = cmsBaseURL+resource+"/?"+query;
        exchange.getOut().setHeader("Authorization", authorizationHdr);
        exchange.getOut().setHeader(Exchange.HTTP_METHOD, httpMethod);
        exchange.setProperty("location", location);
    }
}
