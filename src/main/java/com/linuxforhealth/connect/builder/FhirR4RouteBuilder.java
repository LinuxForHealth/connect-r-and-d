/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "fhir-r4";
    public final static String ROUTE_PRODUCER_ID = "fhir-r4-producer-store-and-notify";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.fhir-r4";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI fhirBaseUri = URI.create(contextSupport.getProperty("lfh.connect.fhir-r4.uri"));

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId(ROUTE_ID)
                .unmarshal().fhirJson("R4")
                .marshal().fhirJson("R4")
                .process(new MetaDataProcessor(routePropertyNamespace))
                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                .id(ROUTE_PRODUCER_ID);
    }
}
