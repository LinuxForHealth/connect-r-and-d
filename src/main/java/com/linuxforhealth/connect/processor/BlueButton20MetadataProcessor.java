/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.time.Instant;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

/**
 * Set the headers used by downstream processors and components
 */
public class BlueButton20MetadataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String blueButtonBaseUri = SimpleBuilder
                .simple("${lfh.connect.bluebutton_20_rest.baseUri}")
                .evaluate(exchange, String.class);

        String resourceType = exchange.getIn().getHeader("resource", String.class);

        String kafkaDataStoreUri = SimpleBuilder
                .simple("{{lfh.connect.datastore.uri}}")
                .evaluate(exchange, String.class)
                .replaceAll("<topicName>", "FHIR_R4_" + resourceType);

        // Form the incoming route url for the message property routeUrl
        String routeUrl = blueButtonBaseUri+"/"+resourceType;
        String queryStr = exchange.getIn().getHeader("CamelHttpQuery", String.class);
        if (queryStr != null && queryStr != "") routeUrl += "?"+queryStr;

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", routeUrl);
        exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "fhir-r4");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("resourceType", resourceType);
    }
}
