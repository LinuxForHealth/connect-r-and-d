/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import ca.uhn.hl7v2.model.Message;
import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import java.time.Instant;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.codec.binary.Base64;

/**
 * Set the headers used by downstream processors and components
 */
public class Hl7v2MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
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

        // Base64-encode the HL7v2 message to allow JSON parsing
        Message msg = exchange.getIn().getBody(Message.class);
        String msgStr = msg.encode();
        String result = Base64.encodeBase64String(msgStr.getBytes());
        exchange.getIn().setBody(result);
    }
}
