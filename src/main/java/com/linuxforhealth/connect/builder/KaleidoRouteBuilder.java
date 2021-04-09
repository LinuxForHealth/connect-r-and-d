/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import com.linuxforhealth.connect.processor.MetaDataProcessor;

import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines example routes
 */
public class KaleidoRouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(KaleidoRouteBuilder.class);

    public final static String KALEIDO_ROUTE_ID = "kaleido";
    public final static String KALEIDO_PRODUCER_ID = "kaleido_producer";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.kaleido";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        /**
         * "Hello World" example route.
         */
        // from("{{lfh.connect.kaleido.uri}}")
        //     .routeId(KALEIDO_ROUTE_ID)
        //     .process(exchange -> {
        //         String name = simple("${headers.name}").evaluate(exchange, String.class);
        //         String result = "Hello Kaleido! It's "+name+".";
        //         // Add your code here
        //         exchange.getIn().setBody(result);
        //     })
        //     .process(new MetaDataProcessor(routePropertyNamespace))
        //     .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        //     .id(KALEIDO_PRODUCER_ID);

         rest("{{lfh.connect.kaleido.uri}}")
            .post("/{resource}")
            .route()
            .routeId(KALEIDO_ROUTE_ID)
            .unmarshal().json(JsonLibrary.Jackson)
            .marshal().json(JsonLibrary.Jackson)
            .process(new MetaDataProcessor(routePropertyNamespace))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
            .id(KALEIDO_PRODUCER_ID);         
    }
}
