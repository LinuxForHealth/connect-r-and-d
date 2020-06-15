/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.LinuxForHealthProcessor;
import java.net.URI;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.engine.DefaultUuidGenerator;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Set the headers used by downstream processors and components
 */
public class SetFhirR4MetadataProcessor extends LinuxForHealthProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(SetFhirR4MetadataProcessor.class);

    @Override
    public void process(Exchange exchange)  {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        URI fhirBaseUri = URI.create(uriBuilder.getFhirR4RestUri());
        Resource resource = (Resource) exchange.getIn().getBody();
        String resourceType = resource.getResourceType().toString();
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_"+resourceType.toUpperCase());
        DefaultUuidGenerator uuidGen = new DefaultUuidGenerator();

        exchange.getIn().setHeader("routeUrl", fhirBaseUri);
        exchange.getIn().setHeader("dataStoreUrl", kafkaDataStoreUri);
        exchange.getIn().setHeader("dataFormat", "fhir-r4");
        exchange.getIn().setHeader("uuid", uuidGen.generateSanitizedId());
        exchange.getIn().setHeader("resourceType", resourceType);
        exchange.getIn().setHeader(KafkaConstants.KEY, "Camel");
    }
}
