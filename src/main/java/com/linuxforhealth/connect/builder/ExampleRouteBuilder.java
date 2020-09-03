/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import com.linuxforhealth.connect.processor.MetaDataProcessor;

import org.apache.camel.Exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines example routes
 */
public class ExampleRouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(ExampleRouteBuilder.class);

    public final static String HELLO_WORLD_ROUTE_ID = "hello-world";
    public final static String HELLO_WORLD_PRODUCER_ID = "hello-world-producer";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.example";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        /**
         * "Hello World" example route.
         */
        from("{{lfh.connect.example.uri}}")
            .routeId(HELLO_WORLD_ROUTE_ID)
            .process(exchange -> {
                String name = simple("${headers.name}").evaluate(exchange, String.class);
                String result = "Hello World! It's "+name+".";
                // Add your code here
                exchange.getIn().setBody(result);
            })
            .process(new MetaDataProcessor(routePropertyNamespace))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
            .id(HELLO_WORLD_PRODUCER_ID);
    }
}
