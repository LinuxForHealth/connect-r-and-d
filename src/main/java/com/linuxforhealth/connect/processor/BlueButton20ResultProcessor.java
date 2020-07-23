/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert the Blue Button 2.0 query result from R3 to R4.
 */
public class BlueButton20ResultProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(BlueButton20ResultProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Resource resource = (Resource) exchange.getIn().getBody();
        String result;

        // Converting Blue Button 2.0 query results to R4
        try {
            IBaseResource converted = VersionConvertor_30_40.convertResource(resource, true);
            result = FhirContext.forR4().newJsonParser().encodeResourceToString(converted);
        } catch(Exception ex) {
            logger.info("Conversion of ExplanationOfBenefit to R4 has not been implemented in HL7 convertors, returning as R3.");
            result = FhirContext.forDstu3().newJsonParser().encodeResourceToString(resource);

            // Set the message attributes for data format back to r3 and change the Kafka queue
            String resourceType = exchange.getProperty("resourceType", String.class);

            String kafkaDataStoreUri = SimpleBuilder
                    .simple("{{lfh.connect.datastore.uri}}")
                    .evaluate(exchange, String.class)
                    .replaceAll("<topicName>", "FHIR_R3_" + resourceType);

            exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
            exchange.setProperty("dataFormat", "fhir-r3");
        }

        exchange.getIn().setBody(result);
    }
}
