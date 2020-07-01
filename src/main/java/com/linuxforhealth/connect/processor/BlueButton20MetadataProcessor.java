/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Set the headers used by downstream processors and components
 */
public class BlueButton20MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String blueButtonBaseUri = uriBuilder.getBlueButton20RestUri();
        String resource = exchange.getIn().getHeader("resource", String.class);
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_"+resource.toUpperCase());

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", blueButtonBaseUri);
        exchange.setProperty("dataStoreUrl", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "fhir-r4");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("resourceType", resource);
    }
}
