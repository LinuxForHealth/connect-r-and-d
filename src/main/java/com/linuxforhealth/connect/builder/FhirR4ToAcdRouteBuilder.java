/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConversionException;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract unstructured data for Annotator for Clinical Data (ACD) analysis
 * 
 * INPUT: FHIR R4 Resources: DocumentReference, more to come ...
 * OUTPUT: unstructured text/plain text
 */
public class FhirR4ToAcdRouteBuilder extends BaseRouteBuilder {

	public final static String FHIR_R4_TO_ACD_CONSUMER_URI = "direct:fhir-r4-to-acd";
	public final static String FHIR_R4_TO_ACD_ROUTE_ID = "fhir-r4-to-acd";
	
	private final Logger logger = LoggerFactory.getLogger(FhirR4ToAcdRouteBuilder.class);

	@Override
	protected String getRoutePropertyNamespace() {
		return "lfh.connect.fhir-acd";
	}

	@Override
	protected void buildRoute(String routePropertyNamespace) {
		
		from(FHIR_R4_TO_ACD_CONSUMER_URI)
		.routeId(FHIR_R4_TO_ACD_ROUTE_ID)
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		.choice()
			.when(body().isNotNull())
				
				.process(exchange -> {
					
					Resource r = null;
					
					try {
						r = exchange.getIn().getBody(Resource.class);
						
					} catch (TypeConversionException e) {
						logger.error("Error retrieving message body as FHIR R4 resource", e);
					}
					
					if (r.getResourceType() == null) {
						logger.warn("FHIR R4 resource type is null - cannot process message");
						return;
					}
					
					// Support multiple fhir resource types (eventually)
					switch(r.getResourceType().toString()) {
					
						case "DocumentReference":
							processDocumentReference(exchange);
							break;
							
						default:
							logger.warn("FHIR R4 resource type not supported: " + r.getResourceType());
					
					}
				})
				
			.otherwise()
				.log(LoggingLevel.WARN, logger, "Received message body was null - unable to process message")
		.end();
		
	}
	
	/**
	 * Process FHIR R4 DocumentReference resources
	 * @param exchange
	 * @throws Exception
	 */
	private void processDocumentReference(Exchange exchange) throws Exception {
		
		DocumentReference docRef = exchange.getIn().getBody(DocumentReference.class);
		
		for (DocumentReferenceContentComponent content:docRef.getContent()) {
			
			Exchange contentExchange = exchange.copy();
			
			contentExchange.getIn().setBody(content.getAttachment().getData(), String.class);
			contentExchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
			
			ProducerTemplate pt = exchange.getContext().createProducerTemplate();
			pt.send("direct:acd-analyze", contentExchange);
		}
		
	}
	
}
