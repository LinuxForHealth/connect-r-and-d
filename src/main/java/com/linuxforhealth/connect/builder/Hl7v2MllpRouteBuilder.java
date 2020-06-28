/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.Hl7v2MetadataProcessor;
import com.linuxforhealth.connect.processor.FormatMessageProcessor;
import com.linuxforhealth.connect.processor.FormatNotificationProcessor;
import com.linuxforhealth.connect.processor.FormatErrorProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends LinuxForHealthRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(Hl7v2MllpRouteBuilder.class);

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        String consumerUri = uriBuilder.getHl7V2MllpUri();
        String producerUri = uriBuilder.getDataStoreUri("HL7v2_${headers[CamelHL7MessageType]}");
        String messagingUri = uriBuilder.getMessagingUri();

        Processor setHl7Metadata = new Hl7v2MetadataProcessor();
        Processor formatMessage = new FormatMessageProcessor();
        Processor formatNotification = new FormatNotificationProcessor();
        Processor formatError = new FormatErrorProcessor();

        from(consumerUri)
                .routeId("hl7-v2-mllp")
                .unmarshal().hl7()
                .process(setHl7Metadata)
                .to("direct:storeandnotify");
    }
}
