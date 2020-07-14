package com.linuxforhealth.connect.builder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linuxforhealth.connect.processor.FhirR4ToAcdProcessor;

/**
 * Extract unstructured data for Annotator for Clinical Data (ACD) analysis
 * 
 * INPUT: FHIR R4 Resources: DocumentReference, more to come ...
 * OUTPUT: unstructured text/plain text
 */
public class FhirR4ToAcdRouteBuilder extends LinuxForHealthRouteBuilder {
	
	private final Logger logger = LoggerFactory.getLogger(FhirR4ToAcdProcessor.class);
	
	Processor fhirR4ToAcdProcessor = new FhirR4ToAcdProcessor();

	@Override
	public void configure() throws Exception {
		
		from("direct:fhir-r4-to-acd")
		
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		
		.choice()
		
			.when(body().isNotNull())
				.process(fhirR4ToAcdProcessor)
			
			.otherwise()
				.log(LoggingLevel.WARN, logger, "Received message body was null - unable to process message")
			
		.end()
		
		;

	}

}
