/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.codec.binary.Base64;

import java.time.Instant;
import java.util.UUID;

/**
 * Set the headers used by downstream processors and components
 */
public class Hl7v2MetadataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        CamelContextSupport contextSupport = new CamelContextSupport(exchange.getContext());

        String consumerUrl = contextSupport.getProperty("lfh.connect.hl7_v2_mllp.uri");

        String resourceType = exchange.getIn()
                .getHeader("CamelHL7MessageType", String.class)
                .toUpperCase();

        String dataStoreUri = contextSupport
                .getProperty("lfh.connect.dataStore.uri")
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
