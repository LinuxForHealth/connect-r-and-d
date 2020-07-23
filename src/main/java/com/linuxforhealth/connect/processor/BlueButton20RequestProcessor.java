/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

/**
 * Set up the Blue Button 2.0 API query
 */
public class BlueButton20RequestProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String cmsBaseURI = SimpleBuilder
                .simple("${lfh.connect.bluebutton_20.cmsBaseUri}")
                .evaluate(exchange, String.class);
        
        // Set up Blue Button 2.0 query
        String authorizationHdr =  exchange.getIn().getHeader("Authorization", String.class);
        String httpMethod = exchange.getIn().getHeader("CamelHttpMethod", String.class);
        String resource = exchange.getIn().getHeader("resource", String.class);
        String query = exchange.getIn().getHeader("CamelHttpQuery", String.class);
        String location = cmsBaseURI+resource+"/?"+query;
        exchange.getOut().setHeader("Authorization", authorizationHdr);
        exchange.getOut().setHeader(Exchange.HTTP_METHOD, httpMethod);
        exchange.setProperty("location", location);
    }
}
