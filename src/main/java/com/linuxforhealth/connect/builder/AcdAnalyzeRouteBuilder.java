/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotator for Clinical Data (ACD) Route for detecting medical concepts in unstructured text via NLP analysis
 *
 * INPUT: text/plain or application/json set by previous route
 * OUTPUT: ACD_INSIGHTS kafka topic message
 */
public class AcdAnalyzeRouteBuilder extends BaseRouteBuilder {

	public final static String ACD_ANALYZE_CONSUMER_URI = "direct:acd-analyze";
	public final static String ACD_ANALYZE_ROUTE_ID = "acd-analyze";
	public final static String ACD_ANALYZE_REQUEST_PRODUCER_ID = "acd-analyze-request-producer";
	public final static String ACD_ANALYZE_PRODUCER_ID = "acd-analyze-producer";

	private final Logger logger = LoggerFactory.getLogger(AcdAnalyzeRouteBuilder.class);

	// Validate required content-type header
	private final Predicate isInvalidContentType = PredicateBuilder.not(
			PredicateBuilder.or(
					header("content-type").contains("text/plain"),
					header("content-type").contains("application/json")));

	// Create predicates to check empty property settings
	Predicate isPropertyNotSet(String propertyName) { return simple("${properties:" + propertyName + "} == ''"); }

	@Override
	protected String getRoutePropertyNamespace() {return "lfh.connect.acd";}

	@Override
	protected void buildRoute(String routePropertyNamespace) {
		from(ACD_ANALYZE_CONSUMER_URI)
		.routeId(ACD_ANALYZE_ROUTE_ID)
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		.log(LoggingLevel.DEBUG, logger, "Received message content-type: ${header.content-type}")

		.choice()

			// ACD request pre-conditions

			.when(isPropertyNotSet("lfh.connect.acd.baseuri"))
				.log(LoggingLevel.WARN, logger, "ACD service endpoint not configured - message will not be processed")
				.stop()

			.when(isPropertyNotSet("lfh.connect.acd.version"))
				.log(LoggingLevel.WARN, logger, "ACD service annotator flow not configured - message will not be processed")
				.stop()

			.when(isPropertyNotSet("lfh.connect.acd.flow"))
				.log(LoggingLevel.WARN, logger, "ACD service version param not configured - message will not be processed")
				.stop()

			.when(isPropertyNotSet("lfh.connect.acd.auth"))
				.log(LoggingLevel.WARN, logger, "ACD service authentication not configured - message will not be processed")
				.stop()

			.when(header("content-type").isNull())
				.log(LoggingLevel.WARN, logger, "ACD request content-type header not set in previous route - message will not be processed")
				.stop()

			.when(isInvalidContentType)
				.log(LoggingLevel.WARN, logger, "Invalid ACD content-type: ${in.header.content-type}. Only text/plain or application/json is supported - - message will not be processed")
				.stop()

			.otherwise() // Cleared to make ACD request
				.setHeader(Exchange.HTTP_METHOD, constant("POST")) // POST /analyze/{flow_id}
				.to("{{lfh.connect.acd.uri}}")
				.id(ACD_ANALYZE_REQUEST_PRODUCER_ID)
				.log(LoggingLevel.DEBUG, logger, "ACD response code: ${header.CamelHttpResponseCode}")
				.unmarshal().json()
				.log(LoggingLevel.DEBUG, logger, "ACD response message body: ${body}")
				.process(new MetaDataProcessor(getRoutePropertyNamespace()))
				.to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
				.id(ACD_ANALYZE_PRODUCER_ID)
				.stop()
		.end();
	}
}
