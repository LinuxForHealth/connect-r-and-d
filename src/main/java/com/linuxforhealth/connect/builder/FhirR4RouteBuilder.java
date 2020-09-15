/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import java.net.URI;
import java.util.Base64;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
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
         * sending FHIR resources to Linux for Health. 
         
         * Set the lfh.connect.fhir-r4.externalservers property to a comma-delimited list of servers to 
         * configure this feature.  Example:
         * lfh.connect.fhir-r4.externalservers=http://localhost:9081/fhir-server/api/v4,http://localhost:9083/fhir-server/api/v4
         *
         * If lfh.connect.fhir-r4.externalservers does not exist or is not defined, no exception will be logged.
         */ 
        from(EXTERNAL_FHIR_ROUTE_URI)
        .routeId(EXTERNAL_FHIR_ROUTE_ID)
        .choice()
			.when(simple("${properties:lfh.connect.fhir-r4.externalservers:doesnotexist} == 'doesnotexist' || ${properties:lfh.connect.fhir-r4.externalservers:doesnotexist} == ''"))
				.stop()
        .end()
        .process(exchange -> {
            String baseURIs = simple("{{lfh.connect.fhir-r4.externalservers}}").evaluate(exchange, String.class);
            String resource = exchange.getIn().getHeader("resource", String.class);
            String[] uris = baseURIs.split(",");
            String headerStr = "";

            // Save off the existing result in a property
            exchange.setProperty("result", exchange.getIn().getBody(String.class));
            
            // Form the URIs for this resource type from the baseURIs
            for (String uri : uris) {
                if (!uri.endsWith("/")) uri += "/";
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
            exchange.getIn().setHeader("Prefer", "return=OperationOutcome");
            logger.info("Sending message to recipientList {}", headerStr);
            exchange.getIn().setHeader("recipientList", headerStr);
        })
        .recipientList(header("recipientList"))
        .ignoreInvalidEndpoints()
        .aggregationStrategy(new AggregationStrategy() {
            public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                if (oldExchange == null) {
                    // Get the original result - the data store location
                    String result = newExchange.getProperty("result", String.class);
                    JSONObject resultObj = new JSONObject(result);

                    // Add the location header to the new result
                    JSONObject msg = addHeaderToResult(newExchange);

                    // Create JSONObject[] and add new result to it
                    JSONObject[] resultArray = new JSONObject[1];
                    resultArray[0] = msg;

                    // Place both results in a new JSON object & set that as the result
                    JSONObject metaResult = new JSONObject();
                    metaResult.put("DataStoreResult", resultObj);
                    metaResult.put("ExternalServerResult", resultArray);
                    newExchange.getIn().setBody(metaResult.toString());
                    return newExchange;
                }

                // Get the new result and add the location header
                JSONObject msg = addHeaderToResult(newExchange);

                // Get the meta result from the old exchange, add new result and set as body
                JSONObject metaResult = new JSONObject(oldExchange.getIn().getBody(String.class));
                JSONObject newMeta = metaResult.append("ExternalServerResult", msg);
                oldExchange.getIn().setBody(newMeta.toString());
                return oldExchange;
            }
        })
        .id(EXTERNAL_FHIR_PRODUCER_ID);
    }

    private JSONObject addHeaderToResult(Exchange exchange) {
        JSONObject msg = new JSONObject();
        JSONObject result = new JSONObject(exchange.getIn().getBody(String.class));
        msg.put("Location", exchange.getIn().getHeader("Location", String.class));
        msg.put("Result", result);
        return msg;
    }
}
