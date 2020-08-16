/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.BlueButton20MetadataProcessor;
import com.linuxforhealth.connect.processor.BlueButton20RequestProcessor;
import com.linuxforhealth.connect.processor.BlueButton20ResultProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Defines routes used to integrate with the Blue Button 2.0 CMS APIs for patient Medicare data.
 *
 * Patient Medicare data is secured using OAuth2 authorization grants. The {@link BlueButton20RestRouteBuilder#AUTHORIZE_ROUTE_ID}
 * and {@link BlueButton20RestRouteBuilder#CALLBACK_ROUTE_ID} are used to support the OAuth2 authorization grant workflow.
 * The {@link BlueButton20RestRouteBuilder#API_ROUTE_ID} route supports data access once a token is attained.
 *
 * The current implementation provides a means for manual integration as the OAuth2 interactions are not yet encapsulated.
 * Once the OAuth2 interactions are encapsulated, this route may extend {@link BaseRouteBuilder}
 */
public class BlueButton20RestRouteBuilder extends RouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-rest-authorize";
    public final static String AUTHORIZE_PRODUCER_ID = "bluebutton-20-authorize-producer";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-rest-callback";
    public final static String CALLBACK_PRODUCER_ID = "bluebutton-20-callback-producer";
    public final static String API_ROUTE_ID = "bluebutton-20-rest";
    public final static String API_ROUTE_PRODUCER_ID = "bluebutton-20-rest-store-producer";
    public final static String API_ROUTE_ERROR_PRODUCER_ID = "bluebutton-20-rest-error-producer";

    private final Logger logger = LoggerFactory.getLogger(BlueButton20RestRouteBuilder.class);


    @Override
    public void configure() {

        CamelContextSupport contextSupport = new CamelContextSupport(getContext());

        URI blueButtonUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.rest.uri"));
        restConfiguration()
                .host(blueButtonUri.getHost())
                .port(blueButtonUri.getPort());

        // Blue Button OAuth2 - Authorize route in Blue Button 2.0 & get code
        URI blueButtonAuthorizeUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.authorizeUri"));
        rest(blueButtonAuthorizeUri.getPath())
                .get()
                .route()
                .routeId(AUTHORIZE_ROUTE_ID)
                .process(exchange -> {
                    String callbackURL = SimpleBuilder
                            .simple("{{lfh.connect.bluebutton_20.handlerUri}}")
                            .evaluate(exchange, String.class);

                    String cmsAuthorizeURL = SimpleBuilder
                            .simple("{{lfh.connect.bluebutton_20.cms.authorizeUri}}")
                            .evaluate(exchange, String.class);

                    String clientId = SimpleBuilder
                            .simple("{{lfh.connect.bluebutton_20.cms.clientId}}")
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

        // Blue Button OAuth2 - Callback to exchange code for token (displayed in the browser)
        URI blueButtonHandlerUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.handlerUri"));
        URI cmsTokenURL = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.cms.tokenUri"));
        rest(blueButtonHandlerUri.getPath())
                .get()
                .route()
                .routeId(CALLBACK_ROUTE_ID)
                .process(exchange -> {

                    String clientId = SimpleBuilder.simple("${properties:lfh.connect.bluebutton_20.cms.clientId}")
                            .evaluate(exchange, String.class);

                    String clientSecret = SimpleBuilder.simple("${properties:lfh.connect.bluebutton_20.cms.clientSecret}")
                            .evaluate(exchange, String.class);

                    // Setting up call to Blue Button 2.0 to exchange the code for a token
                    String code  = exchange.getIn().getHeader("code", String.class);
                    String body = "code="+code+"&grant_type=authorization_code";
                    String auth = clientId+":"+clientSecret;
                    String authHeader = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                    exchange.getOut().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getOut().setHeader("Authorization", authHeader);
                    exchange.getOut().setHeader("Content-Type", "application/x-www-form-urlencoded");
                    exchange.getOut().setHeader("Content-Length", body.length());
                    exchange.getOut().setBody(body);
                })
                .to(cmsTokenURL.toString())
                .id(CALLBACK_PRODUCER_ID);

        // Blue Button 2.0 route - Retrieve patient resources
        rest(blueButtonUri.getPath())
                .get("/{resource}")
                .route()
                .routeId(API_ROUTE_ID)
                .process(new BlueButton20MetadataProcessor())
                .doTry()
                    .process(new BlueButton20RequestProcessor())
                    .toD("${exchangeProperty[location]}")
                    .unmarshal().fhirJson("DSTU3")
                    .process(new BlueButton20ResultProcessor())
                    .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                    .id(API_ROUTE_PRODUCER_ID)
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI)
                    .id(API_ROUTE_ERROR_PRODUCER_ID)
                .end();
    }
}
