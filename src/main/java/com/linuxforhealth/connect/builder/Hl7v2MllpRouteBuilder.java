/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "hl7-v2-mllp";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.hl7_v2_mllp";

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        from("{{lfh.connect.hl7_v2_mllp.uri}}")
                .routeId(ROUTE_ID)
                .unmarshal().hl7()
                .process(new MetaDataProcessor(routePropertyNamespace))
                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
