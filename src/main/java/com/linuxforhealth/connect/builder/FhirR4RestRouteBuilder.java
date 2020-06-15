/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.SetFhirR4MetadataProcessor;
import com.linuxforhealth.connect.processor.FormatMessageProcessor;
import com.linuxforhealth.connect.processor.FormatNotificationProcessor;
import com.linuxforhealth.connect.processor.FormatErrorProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
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
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_${headers.resourceType.toUpperCase()}");
        String messagingUri = uriBuilder.getMessagingUri();

        Processor setFhirR4Metadata = new SetFhirR4MetadataProcessor();
        Processor formatMessage = new FormatMessageProcessor();
        Processor formatNotification = new FormatNotificationProcessor();
        Processor formatError = new FormatErrorProcessor();

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId("fhir-r4-rest")
                .unmarshal().fhirJson("R4")
                .process(setFhirR4Metadata)
                .marshal().fhirJson("R4")
                .process(formatMessage)
                .doTry()
                    .toD(kafkaDataStoreUri)
                    .process(formatNotification)
                    .to(messagingUri)
                .doCatch(Exception.class)
                   .process(formatError)
                   .log(LoggingLevel.ERROR, logger, "${body}")
                   .to(messagingUri)
                .end();
    }
}
