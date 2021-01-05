/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.Hl7NaaccrProcessor;
import com.linuxforhealth.connect.processor.MetaDataProcessor;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2RouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "hl7-v2";
    public final static String ROUTE_PRODUCER_ID="hl7-v2";
    public final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.hl7-v2";
    public final static String HTTP_ROUTE_ID = "hl7-v2-http";
    public final static String NAACCR_ROUTE_ID = "hl7-v2-naaccr";
    public final static String NAACCR_PRODUCER_ID = "hl7-v2-naaccr-producer";

    private final Logger logger = LoggerFactory.getLogger(Hl7NaaccrProcessor.class);

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        //Route for MLLP origin
        from("{{lfh.connect.hl7-v2.uri}}")
                .routeId(ROUTE_ID)
                .unmarshal().hl7()
                .process(new MetaDataProcessor(routePropertyNamespace))
                .multicast()
                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "direct:"+NAACCR_ROUTE_ID)
                .id(ROUTE_PRODUCER_ID);

        //Route for HTTP origin
        from("{{lfh.connect.hl7-v2.http.uri}}")
                .routeId(HTTP_ROUTE_ID)
                .unmarshal().hl7()
                .process(new MetaDataProcessor(routePropertyNamespace))
                .multicast()
                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "direct:"+NAACCR_ROUTE_ID)
                .id(HTTP_ROUTE_ID);

        //Route for NAACCR HL7 Subprotocol for Electronic Pathology Reports
        from("direct:"+NAACCR_ROUTE_ID)
                .routeId(NAACCR_ROUTE_ID)
                .process(new Hl7NaaccrProcessor(routePropertyNamespace))
                .choice()
                .when(isHeaderSet)
                    .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                    .id(NAACCR_PRODUCER_ID)  
                .otherwise()
                .log(LoggingLevel.INFO, logger, "non-NAACCR report")
                    .stop()
                .end();
       
    
    }

    //used as predicate to detect whether to process as an NAACCR message
    private final Predicate isHeaderSet = header("naaccrReportType").isNotNull();

}
