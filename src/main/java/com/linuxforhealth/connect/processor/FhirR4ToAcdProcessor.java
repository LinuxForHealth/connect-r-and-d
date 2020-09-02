/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConversionException;
import org.apache.camel.support.DefaultExchange;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract unstructured text from applicable FHIR R4 resources
 */
public class FhirR4ToAcdProcessor implements Processor {
	
	private final Logger logger = LoggerFactory.getLogger(FhirR4ToAcdProcessor.class);

	public FhirR4ToAcdProcessor() { }

	@Override
	public void process(Exchange exchange) throws Exception {
		
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
