/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.LoggingLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports X12 Transaction Processing
 */
public class X12RouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(X12RouteBuilder.class);
    public static final String ROUTE_ID = "x12";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.x12";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String x12Uri = ctxSupport.getProperty("lfh.connect.x12.uri");

        rest(x12Uri)
        .post()
        .route()
        .routeId(ROUTE_ID)
        .convertBodyTo(String.class)
        .log(LoggingLevel.DEBUG, logger, "Received x12 ${body}")
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
