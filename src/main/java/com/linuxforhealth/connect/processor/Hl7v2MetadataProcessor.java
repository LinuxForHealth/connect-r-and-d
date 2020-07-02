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

/**
 * Set the headers used by downstream processors and components
 */
public class Hl7v2MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String consumerUrl = uriBuilder.getHl7V2MllpUri();
        String resourceType = exchange.getIn().getHeader("CamelHL7MessageType", String.class);
        String dataStoreUri = uriBuilder.getDataStoreUri("HL7v2_"+resourceType.toUpperCase());

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", consumerUrl);
        exchange.setProperty("dataStoreUri", dataStoreUri);
        exchange.setProperty("dataFormat", "hl7-v2");
        exchange.setProperty("uuid",  UUID.randomUUID());
        exchange.setProperty("resourceType", resourceType);
    }
}
