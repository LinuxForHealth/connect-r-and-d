/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

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
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String fhirUri = ctxSupport.getProperty("lfh.connect.fhir-r4.uri");
        rest(fhirUri)
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
         * Use the Camel Recipient List EIP to optionally send data to one or more external fhir servers
         * when sending FHIR resources to LinuxForHealth.
         
         * Set the lfh.connect.fhir-r4.externalserver property to a fhir server path to
         * enable this feature.  Example:
         * lfh.connect.fhir-r4.externalserver=http://localhost:9081/fhir-server/api/v4
         *
         * If lfh.connect.fhir-r4.externalserver does not exist or is not defined, no exception will be logged.
         */ 
        from(EXTERNAL_FHIR_ROUTE_URI)
        .routeId(EXTERNAL_FHIR_ROUTE_ID)
        .choice()
			.when(simple("${properties:lfh.connect.fhir-r4.externalserver:doesnotexist} == 'doesnotexist'"))
				.stop()
            .when(simple("${properties:lfh.connect.fhir-r4.externalserver} == ''"))
				.stop()
        .end()
        .process(exchange -> {
            String baseURIs = simple("{{lfh.connect.fhir-r4.externalserver}}").evaluate(exchange, String.class);
            String resource = exchange.getIn().getHeader("resource", String.class);
            String[] uris = baseURIs.split(",");
            String headerStr = "";

            // Save off the existing result in a property
            exchange.setProperty("result", exchange.getIn().getBody(String.class));

            // Decode the data and set as message body
            JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
            byte[] body = Base64.getDecoder().decode(msg.getString("data"));
            exchange.getIn().setBody(body);

            // Set up for the recipient list outbound calls
            exchange.getIn().removeHeaders("Camel*");
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
            exchange.getIn().setHeader("Prefer", "return=OperationOutcome");
        })
        .toD("${properties:lfh.connect.fhir-r4.externalserver}/${header[resource]}")
        .id(EXTERNAL_FHIR_PRODUCER_ID);
    }
}
