/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.LinuxForHealthProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.engine.DefaultUuidGenerator;

/**
 * Set the headers used by downstream processors and components
 */
public class SetHl7v2MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String consumerUrl = uriBuilder.getHl7V2MllpUri();
        String resourceType = exchange.getIn().getHeader("CamelHL7MessageType", String.class);
        String dataStoreUrl = uriBuilder.getDataStoreUri("HL7v2_"+resourceType.toUpperCase());
        DefaultUuidGenerator uuidGen = new DefaultUuidGenerator();

        exchange.getIn().setHeader("routeUrl", consumerUrl);
        exchange.getIn().setHeader("dataStoreUrl", dataStoreUrl);
        exchange.getIn().setHeader("dataFormat", "hl7-v2");
        exchange.getIn().setHeader("uuid", uuidGen.generateSanitizedId());
        exchange.getIn().setHeader(KafkaConstants.KEY, "Camel");
    }
}
