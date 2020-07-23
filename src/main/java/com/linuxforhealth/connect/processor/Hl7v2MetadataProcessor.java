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
import org.apache.commons.codec.binary.Base64;

/**
 * Set the headers used by downstream processors and components
 */
public class Hl7v2MetadataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String consumerUrl = SimpleBuilder
                .simple("{{lfh.connect.hl7_v2_mllp.uri}}")
                .evaluate(exchange, String.class);

        String resourceType = exchange.getIn()
                .getHeader("CamelHL7MessageType", String.class)
                .toUpperCase();

        String dataStoreUri = SimpleBuilder
                .simple("{{lfh.connect.datastore.uri}}")
                .evaluate(exchange, String.class)
                .replaceAll("<topicName>", "HL7v2_" + resourceType);

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", consumerUrl);
        exchange.setProperty("dataStoreUri", dataStoreUri);
        exchange.setProperty("dataFormat", "hl7-v2");
        exchange.setProperty("uuid",  UUID.randomUUID());
        exchange.setProperty("resourceType", resourceType);

        // Base64-encode the HL7v2 message to allow JSON parsing
        String result = Base64.encodeBase64String(exchange.getIn().getBody(byte[].class));
        exchange.getIn().setBody(result);
    }
}
