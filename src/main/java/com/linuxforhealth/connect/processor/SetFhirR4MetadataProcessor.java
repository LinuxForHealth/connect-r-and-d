/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import java.net.URI;
import java.time.Instant;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.engine.DefaultUuidGenerator;
import org.hl7.fhir.r4.model.Resource;

/**
 * Set the headers used by downstream processors and components
 */
public class SetFhirR4MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        URI fhirBaseUri = URI.create(uriBuilder.getFhirR4RestUri());
        Resource resource = (Resource) exchange.getIn().getBody();
        String resourceType = resource.getResourceType().toString();
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_"+resourceType.toUpperCase());
        DefaultUuidGenerator uuidGen = new DefaultUuidGenerator();

        exchange.getIn().setHeader("timestamp", Instant.now().getEpochSecond());
        exchange.getIn().setHeader("routeUrl", fhirBaseUri);
        exchange.getIn().setHeader("dataStoreUrl", kafkaDataStoreUri);
        exchange.getIn().setHeader("dataFormat", "fhir-r4");
        exchange.getIn().setHeader("uuid", uuidGen.generateSanitizedId());
        exchange.getIn().setHeader("resourceType", resourceType);
        exchange.getIn().setHeader(KafkaConstants.KEY, "Camel");
    }
}
