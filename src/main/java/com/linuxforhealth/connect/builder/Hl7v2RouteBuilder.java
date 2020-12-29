/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.Hl7NaaccrProcessor;
import com.linuxforhealth.connect.processor.MetaDataProcessor;


/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2RouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "hl7-v2";
    public final static String ROUTE_PRODUCER_ID="hl7-v2";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.hl7-v2";
    public final static String HTTP_ROUTE_ID = "hl7-v2-http";
    public final static String NAACCR_ROUTE_ID = "hl7-v2-naaccr";

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
            .to(LinuxForHealthRouteBuilder.STORE_CONSUMER_URI)
            .id(NAACCR_ROUTE_ID);        
       
    }
}
