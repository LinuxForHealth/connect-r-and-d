/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import java.time.Instant;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;

/**
 * Set the headers used by downstream processors and components
 */
public class Hl7v2MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String consumerUrl = uriBuilder.getHl7V2MllpUri();
        String resourceType = exchange.getIn().getHeader("CamelHL7MessageType", String.class);
        String dataStoreUrl = uriBuilder.getDataStoreUri("HL7v2_"+resourceType.toUpperCase());

        exchange.getIn().setHeader("timestamp", Instant.now().getEpochSecond());
        exchange.getIn().setHeader("routeUrl", consumerUrl);
        exchange.getIn().setHeader("dataStoreUrl", dataStoreUrl);
        exchange.getIn().setHeader("dataFormat", "hl7-v2");
        exchange.getIn().setHeader("uuid",  UUID.randomUUID());
        exchange.getIn().setHeader(KafkaConstants.KEY, "Camel");
    }
}
