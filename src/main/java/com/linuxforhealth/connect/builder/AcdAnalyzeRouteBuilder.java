/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linuxforhealth.connect.processor.AcdAnalyzeProcessor;

/**
 * Annotator for Clinical Data (ACD) Route for detecting medical concepts in unstructured text via NLP analysis
 * 
 * INPUT: text/plain or application/json set by previous route
 * OUTPUT: ACD_INSIGHTS kafka topic message
 */
public class AcdAnalyzeRouteBuilder extends LinuxForHealthRouteBuilder {
	
	private final Logger logger = LoggerFactory.getLogger(AcdAnalyzeRouteBuilder.class);
	
	private final Processor acdAnalyzeProcessor = new AcdAnalyzeProcessor();
	
	// Choice predicates for ACD request pre-conditions
	private final Predicate baseUriNotSet = simple("${properties:linuxforhealth.connect.endpoint.acd_rest.baseUri} == ''");
	private final Predicate versionParamNotSet = simple("${properties:linuxforhealth.connect.endpoint.acd_rest.version} == ''");
	private final Predicate flowNotSet = simple("${properties:linuxforhealth.connect.endpoint.acd_rest.flow} == ''");
	private final Predicate keyNotSet = simple("${properties:linuxforhealth.connect.endpoint.acd_rest.key} == ''");
	private final Predicate contentTypeNotSet = header("content-type").isNull();
	private final Predicate invalidContentType = PredicateBuilder.not(
													PredicateBuilder.or(
															header("content-type").contains("text/plain"),
															header("content-type").contains("application/json")));

	@Override
	public void configure() throws Exception {
        
        from("direct:acd-analyze")
        
        .log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
        .log(LoggingLevel.DEBUG, logger, "Received message content-type: ${header.content-type}")
        
        .choice()
        
        	/*
        	 * QUESTION: should there be an on/off property for whether to enable ACD?
        	 * 	If this property was set to false or not set at all, we could just check that first
        	 * 	and stop message processing with only a debug statement instead of these
        	 * 	warning messages (users will only see one per message route).
        	 */
        
        	// ACD request pre-conditions
        
        	.when(baseUriNotSet)
        		.log(LoggingLevel.WARN, logger, "ACD service endpoint not configured - message will not be processed")
        		.stop()
        		
        	.when(flowNotSet)
        		.log(LoggingLevel.WARN, logger, "ACD service annotator flow not configured - message will not be processed")
        		.stop()
        		
        	.when(versionParamNotSet)
        		.log(LoggingLevel.WARN, logger, "ACD service version param not configured - message will not be processed")
        		.stop()
        		
        	.when(keyNotSet)
        		.log(LoggingLevel.WARN, logger, "ACD service apikey not configured - message will not be processed")
        		.stop()
        
        	.when(contentTypeNotSet)
        		.log(LoggingLevel.WARN, logger, "ACD request content-type header not set in previous route - message will not be processed")
        		.stop()
        		
        	.when(invalidContentType)
        		.log(LoggingLevel.WARN, logger, "Invalid ACD content-type: ${in.header.content-type}. Only text/plain or application/json is supported - - message will not be processed")
        		.stop()
        		
        	.otherwise() // Cleared to make ACD request

	        	.setHeader(Exchange.HTTP_METHOD, constant("POST")) // POST /analyze/{flow_id}
	        	
	        	.doTry()
	            
		            .to("{{linuxforhealth.connect.endpoint.acd_rest.baseUri}}/v1/analyze/{{linuxforhealth.connect.endpoint.acd_rest.flow}}" +
		            		"?version={{linuxforhealth.connect.endpoint.acd_rest.version}}" +
		            		"&authMethod=Basic" +
		    				"&authUsername=apikey" +
		    				"&authPassword={{linuxforhealth.connect.endpoint.acd_rest.key}}")
		            
		            .log(LoggingLevel.DEBUG, logger, "ACD response code: ${header.CamelHttpResponseCode}")
		            
		            .unmarshal().json()
		            
		            .log(LoggingLevel.DEBUG, logger, "ACD response messge body: ${body}")
		            
		            .process(acdAnalyzeProcessor)
		            
		            .to("direct:storeandnotify")

		        .doCatch(HttpOperationFailedException.class) // ACD error response handling
		        
		        	.log(LoggingLevel.ERROR, logger, "ACD error response code: ${header.CamelHttpResponseCode}")
		        	.log(LoggingLevel.ERROR, logger, "ACD error response message: ${header.CamelHttpResponseText}")
		        	.stop()
		        
		        .end()
	            
        .end()
        
        ;

	}

}
