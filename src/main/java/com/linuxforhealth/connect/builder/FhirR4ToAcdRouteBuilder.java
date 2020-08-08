/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.FhirR4ToAcdProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract unstructured data for Annotator for Clinical Data (ACD) analysis
 * 
 * INPUT: FHIR R4 Resources: DocumentReference, more to come ...
 * OUTPUT: unstructured text/plain text
 */
public class FhirR4ToAcdRouteBuilder extends RouteBuilder {

	public final static String FHIR_R4_TO_ACD_CONSUMER_URI = "direct:fhir-r4-to-acd";
	public final static String FHIR_R4_TO_ACD_ROUTE_ID = "fhir-r4-to-acd";
	
	private final Logger logger = LoggerFactory.getLogger(FhirR4ToAcdProcessor.class);
	
	@Override
	public void configure() {
		
		from(FHIR_R4_TO_ACD_CONSUMER_URI)
		.routeId(FHIR_R4_TO_ACD_ROUTE_ID)
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		.choice()
			.when(body().isNotNull())
				.process(new FhirR4ToAcdProcessor())
			.otherwise()
				.log(LoggingLevel.WARN, logger, "Received message body was null - unable to process message")
		.end();
	}
}
