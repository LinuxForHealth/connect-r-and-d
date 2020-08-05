/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.FhirR4MetadataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RestRouteBuilder extends RouteBuilder {

    public final static String FHIR_R4_ROUTE_ID = "fhir-r4-rest";

    @Override
    public void configure() {

        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI fhirBaseUri = URI.create(contextSupport.getProperty("lfh.connect.fhir_r4_rest.uri"));

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId(FHIR_R4_ROUTE_ID)
                .unmarshal().fhirJson("R4")
                .process(new FhirR4MetadataProcessor())
                .to(LinuxForHealthDirectRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
