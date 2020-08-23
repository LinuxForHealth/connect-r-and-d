/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import ca.uhn.fhir.context.FhirContext;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.commons.lang3.SystemUtils;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Defines routes used to integrate with the Blue Button 2.0 CMS APIs for patient Medicare data.
 *
 * Patient Medicare data is secured using OAuth2 authorization grants. The {@link BlueButton20RouteBuilder#AUTHORIZE_ROUTE_ID}
 * and {@link BlueButton20RouteBuilder#CALLBACK_ROUTE_ID} are used to support the OAuth2 authorization grant workflow.
 * The {@link BlueButton20RouteBuilder#API_ROUTE_ID} route supports data access once a token is attained.
 *
 * The current implementation provides a means for manual integration as the OAuth2 interactions are not yet encapsulated.
 * Once the OAuth2 interactions are encapsulated, this route may extend {@link BaseRouteBuilder}
 */
public class BlueButton20RouteBuilder extends RouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-authorize";
    public final static String AUTHORIZE_PRODUCER_ID = "bluebutton-20-authorize-producer";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-callback";
    public final static String CALLBACK_PRODUCER_ID = "bluebutton-20-callback-producer";
    public final static String API_ROUTE_ID = "bluebutton-20";
    public final static String API_ROUTE_BLUE_BUTTON_REQUEST_PRODUCER_ID = "bluebutton-20-request-producer";
    public final static String API_ROUTE_PRODUCER_ID = "bluebutton-20-producer";
    public final static String API_ROUTE_ERROR_PRODUCER_ID = "bluebutton-20-error-producer";

    private final Logger logger = LoggerFactory.getLogger(BlueButton20RouteBuilder.class);

    /**
     * Defines the route used to initiate the browser based Blue Button 2.0 CMS API authorization request.
     * "Step 1" of OAuth2 Authorization
     */
    private void addAuthorizationRoute() {
        // Blue Button OAuth2 - Authorize route in Blue Button 2.0 & get code
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI blueButtonAuthorizeUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton-20.authorizeuri"));
        rest(blueButtonAuthorizeUri.getPath())
            .get()
            .route()
            .routeId(AUTHORIZE_ROUTE_ID)
            .process(exchange -> {
                String callbackURL = SimpleBuilder
                        .simple("${properties:lfh.connect.bluebutton-20.handleruri}")
                        .evaluate(exchange, String.class);

                String cmsAuthorizeURL = SimpleBuilder
                        .simple("${properties:lfh.connect.bluebutton-20.cms.authorizeuri}")
                        .evaluate(exchange, String.class);

                String clientId = SimpleBuilder
                        .simple("${properties:lfh.connect.bluebutton-20.cms.clientid}")
                        .evaluate(exchange, String.class);

                // Set up call to redirect to Blue Button API so the user can authenticate this application
                String authorizeURL = cmsAuthorizeURL +
                        "?client_id=" + clientId+
                        "&redirect_uri=" + callbackURL +
                        "&response_type=code";

                logger.debug("Authorize URL: "+authorizeURL);

                // Determine the current OS so we know the cmd to launch the browser
                String osCmd;
                if (SystemUtils.IS_OS_MAC) {
                    osCmd = "open";
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    osCmd = "explorer";
                } else {
                    // Assume SystemUtils.IS_OS_UNIX
                    osCmd = "xdg-open";
                }
                exchange.setProperty("location", "exec:"+osCmd+"?args=RAW("+authorizeURL+")");
            })
            .toD("${exchangeProperty[location]}")
            .id(AUTHORIZE_PRODUCER_ID);
    }

    /**
     * Defines the route used to request an access token based on an authorization code.
     * "Step 2" of OAuth2 Authorization
     */
    private void addCallbackRoute() {
        // Blue Button OAuth2 - Callback to exchange code for token (displayed in the browser)
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI blueButtonHandlerUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton-20.handleruri"));
        URI cmsTokenURL = URI.create(contextSupport.getProperty("lfh.connect.bluebutton-20.cms.tokenuri"));
        rest(blueButtonHandlerUri.getPath())
                .get()
                .route()
                .routeId(CALLBACK_ROUTE_ID)
                .process(exchange -> {

                    String clientId = SimpleBuilder.simple("${properties:lfh.connect.bluebutton-20.cms.clientid}")
                            .evaluate(exchange, String.class);

                    String clientSecret = SimpleBuilder.simple("${properties:lfh.connect.bluebutton-20.cms.clientsecret}")
                            .evaluate(exchange, String.class);

                    // Setting up call to Blue Button 2.0 to exchange the code for a token
                    String code  = exchange.getIn().getHeader("code", String.class);
                    String body = "code="+code+"&grant_type=authorization_code";
                    String auth = clientId+":"+clientSecret;
                    String authHeader = "Basic "+ Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                    exchange.getOut().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getOut().setHeader("Authorization", authHeader);
                    exchange.getOut().setHeader("Content-Type", "application/x-www-form-urlencoded");
                    exchange.getOut().setHeader("Content-Length", body.length());
                    exchange.getOut().setBody(body);
                })
                .to(cmsTokenURL.toString())
                .id(CALLBACK_PRODUCER_ID)
                .to("log:DEBUG?showBody=true&showHeaders=true");
    }

    /**
     * Defines the route used to request data from the Blue Button 2.0 CMS API.
     * "Step 3" - API requests are made with a valid Authorization token.
     */
    private void addBlueButtonApiRoute(String apiPath) {
        // Blue Button 2.0 route - Retrieve patient resources
        rest(apiPath)
            .get("/{resource}")
            .route()
            .routeId(API_ROUTE_ID)
            .process(exchange -> {
                String blueButtonUri = SimpleBuilder
                        .simple("${properties:lfh.connect.bluebutton-20.uri}")
                        .evaluate(exchange, String.class);

                String resourceType = exchange.getIn().getHeader("resource", String.class).toUpperCase();

                String kafkaDataStoreUri = SimpleBuilder.simple("${properties:lfh.connect.datastore.uri}")
                        .evaluate(exchange, String.class)
                        .replaceAll("<topicName>", "FHIR-R4_" + resourceType);

                // Form the incoming route url for the message property routeUrl
                String routeUri = blueButtonUri+"/"+resourceType;
                String queryStr = exchange.getIn().getHeader("CamelHttpQuery", String.class);
                if (queryStr != null && queryStr != "") routeUri += "?"+queryStr;

                exchange.setProperty("routeId", exchange.getFromRouteId());
                exchange.setProperty("timestamp", Instant.now().getEpochSecond());
                exchange.setProperty("routeUri", routeUri);
                exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
                exchange.setProperty("dataFormat", "FHIR-R4");
                exchange.setProperty("uuid", UUID.randomUUID());
                exchange.setProperty("messageType", resourceType.toUpperCase());
            })
            .doTry()
            .process(exchange -> {
                String cmsBaseURI = SimpleBuilder.simple("${properties:lfh.connect.bluebutton-20.cms.baseuri}")
                        .evaluate(exchange, String.class);

                // Set up Blue Button 2.0 query
                String authorizationHdr =  exchange.getIn().getHeader("Authorization", String.class);
                String httpMethod = exchange.getIn().getHeader("CamelHttpMethod", String.class);
                String resource = exchange.getIn().getHeader("resource", String.class);
                String query = exchange.getIn().getHeader("CamelHttpQuery", String.class);
                String location = cmsBaseURI+resource+"/?"+query;
                exchange.getOut().setHeader("Authorization", authorizationHdr);
                exchange.getOut().setHeader(Exchange.HTTP_METHOD, httpMethod);
                exchange.setProperty("location", location);
            })
            .toD("${exchangeProperty[location]}")
            .id(API_ROUTE_BLUE_BUTTON_REQUEST_PRODUCER_ID)
            .unmarshal().fhirJson("DSTU3")
            .process(exchange -> {
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
                    String resourceType = exchange.getProperty("resourceType", String.class).toUpperCase();

                    String kafkaDataStoreUri = SimpleBuilder.simple("${properties:lfh.connect.datastore.uri}")
                            .evaluate(exchange, String.class)
                            .replaceAll("<topicName>", "FHIR-R3_" + resourceType);

                    exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
                    exchange.setProperty("dataFormat", "fhir-r3");
                }

                String encodedResult = Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8));
                exchange.getIn().setBody(encodedResult);
            })
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
            .id(API_ROUTE_PRODUCER_ID)
            .doCatch(Exception.class)
            .setProperty("errorMessage", simple(exceptionMessage().toString()))
            .to(LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI)
            .id(API_ROUTE_ERROR_PRODUCER_ID)
            .end();
    }


    /**
     * Defines the routes used to support CMS Blue Button 2.0 integration
     */
    @Override
    public void configure() {

        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        URI blueButtonUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton-20.uri"));

        restConfiguration()
                .host(blueButtonUri.getHost())
                .port(blueButtonUri.getPort());

        addAuthorizationRoute();
        addCallbackRoute();
        addBlueButtonApiRoute(blueButtonUri.getPath());
    }
}
