/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.FhirR4MetadataProcessor;
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RestRouteBuilder extends RouteBuilder {

    public final static String FHIR_R4_ROUTE_ID = "fhir-r4-rest";

    @Override
    public void configure() {
        URI fhirBaseUri = URI.create(
                getContext()
                .getPropertiesComponent()
                .resolveProperty("lfh.connect.fhir_r4_rest.uri")
                .orElse("http://0.0.0.0:8080/fhir/r4"));

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId(FHIR_R4_ROUTE_ID)
                .unmarshal().fhirJson("R4")
                .process(new FhirR4MetadataProcessor())
                .to("direct:storeandnotify");
    }
}
