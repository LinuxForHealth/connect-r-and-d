/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.BlueButton20AuthProcessor;
import com.linuxforhealth.connect.processor.BlueButton20CallbackProcessor;
import com.linuxforhealth.connect.processor.BlueButton20MetadataProcessor;
import com.linuxforhealth.connect.processor.BlueButton20RequestProcessor;
import com.linuxforhealth.connect.processor.BlueButton20ResultProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class BlueButton20RestRouteBuilder extends RouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-rest-authorize";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-rest-callback";
    public final static String API_ROUTE_ID = "bluebutton-20-rest";

    private final Logger logger = LoggerFactory.getLogger(BlueButton20AuthProcessor.class);


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
                .toD("${exchangeProperty[location]}");

        // Blue Button OAuth2 - Callback to exchange code for token (displayed in the browser)
        URI blueButtonHandlerUri = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.handlerUri"));
        URI cmsTokenURL = URI.create(contextSupport.getProperty("lfh.connect.bluebutton_20.cms.tokenUri"));
        rest(blueButtonHandlerUri.getPath())
                .get()
                .route()
                .routeId(CALLBACK_ROUTE_ID)
                .doTry()
                    .process(new BlueButton20CallbackProcessor())
                    .to(cmsTokenURL.toString())
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI)
                .end();
        
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
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI)
                .end();
    }
}
