/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.FhirR4MetadataProcessor;
import com.linuxforhealth.connect.processor.FormatMessageProcessor;
import com.linuxforhealth.connect.processor.FormatNotificationProcessor;
import com.linuxforhealth.connect.processor.FormatErrorProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RestRouteBuilder extends LinuxForHealthRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(FhirR4RestRouteBuilder.class);

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        URI fhirBaseUri = URI.create(uriBuilder.getFhirR4RestUri());
        Processor setFhirR4Metadata = new FhirR4MetadataProcessor();

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId("fhir-r4-rest")
                .unmarshal().fhirJson("R4")
                .process(setFhirR4Metadata)
                .to("direct:storeandnotify");
    }
}
