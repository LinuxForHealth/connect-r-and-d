/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RouteBuilder extends BaseRouteBuilder {

     private final Logger logger = LoggerFactory.getLogger(FhirR4RouteBuilder.class);

    public final static String ROUTE_ID = "fhir-r4";
    public final static String ROUTE_PRODUCER_ID = "fhir-r4-producer-store-and-notify";
    public final static String EXTERNAL_FHIR_ROUTE_URI = "direct:toExternalFhirServers";
    public final static String EXTERNAL_FHIR_ROUTE_ID = "external-fhir-servers";
    public final static String EXTERNAL_FHIR_PRODUCER_ID = "lfh-external-fhir-producer";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.fhir-r4";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI fhirBaseUri = URI.create(contextSupport.getProperty("lfh.connect.fhir-r4.uri"));

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId(ROUTE_ID)
                .unmarshal().fhirJson("R4")
                .marshal().fhirJson("R4")
                .process(new MetaDataProcessor(routePropertyNamespace))
                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                .id(ROUTE_PRODUCER_ID)
                .to(EXTERNAL_FHIR_ROUTE_URI);

        /*
         * Use the recipientList eip to optionally send data to one or more external fhir servers when 
         * sending FHIR resources to Linus for Health. 
         
         * Set the lfh.connect.fhir-r4.externalservers property to a comma-delimted list of servers to 
         * configure this feature.  Example:
         * lfh.connect.fhir-r4.externalservers=http://localhost:9081/fhir-server/api/v4,http://localhost:9083/fhir-server/api/v4
         *
         * If lfh.connect.fhir-r4.externalservers does not exist or is not defined, no exception will be logged.
         */ 
        from(EXTERNAL_FHIR_ROUTE_URI)
        .routeId(EXTERNAL_FHIR_ROUTE_ID)
        .doTry()
            .process(exchange -> {
                String baseURIs = simple("{{lfh.connect.fhir-r4.externalservers}}").evaluate(exchange, String.class);
                if (!baseURIs.equals("") && baseURIs != null) {
                    String resource = exchange.getIn().getHeader("resource", String.class);
                    String[] uris = baseURIs.split(",");
                    String headerStr = "";
                    
                    // Form the URIs for this resource type from the baseURIs
                    for (String uri : uris) {
                        if (!uri.substring(uri.length() - 1).equals("/")) uri += "/";
                        uri += resource;
                        if (!headerStr.equals("")) headerStr += ",";
                        headerStr += uri;
                    }

                    // Decode the data and set as message body
                    JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
                    byte[] body = Base64.getDecoder().decode(msg.getString("data"));
                    exchange.getIn().setBody(body);

                    // Set up for the recipient list outbound calls
                    exchange.getIn().removeHeaders("Camel*");
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                    logger.info("Sending message to recipientList {}", headerStr);
                    exchange.getIn().setHeader("recipientList", headerStr);
                }
            })
        .doCatch(CamelExecutionException.class)
        .doCatch(Exception.class)
            .log(LoggingLevel.ERROR, logger, exceptionMessage().toString())
        .end()
        .recipientList(header("recipientList"))
        .ignoreInvalidEndpoints()
        .id(EXTERNAL_FHIR_PRODUCER_ID);
    }
}
