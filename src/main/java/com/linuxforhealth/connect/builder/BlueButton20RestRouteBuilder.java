/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.BlueButton20AuthProcessor;
import com.linuxforhealth.connect.processor.BlueButton20CallbackProcessor;
import com.linuxforhealth.connect.processor.BlueButton20MetadataProcessor;
import com.linuxforhealth.connect.processor.BlueButton20RequestProcessor;
import com.linuxforhealth.connect.processor.BlueButton20ResultProcessor;
import java.net.URI;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class BlueButton20RestRouteBuilder extends LinuxForHealthRouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-rest-authorize";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-rest-callback";
    public final static String API_ROUTE_ID = "bluebutton-20-rest";

    private final Logger logger = LoggerFactory.getLogger(BlueButton20RestRouteBuilder.class);

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        URI blueButtonBaseUri = URI.create(uriBuilder.getBlueButton20RestUri());
        URI blueButtonAuthorizeUri = URI.create(uriBuilder.getBlueButton20RestAuthorizeUri());
        URI blueButtonCallbackUri = URI.create(uriBuilder.getBlueButton20RestCallbackUri());
        String cmsTokenURL = uriBuilder.getBlueButton20CmsTokenUri();
        String messagingUri = uriBuilder.getMessagingUri();

        Processor handleBlueButtonAuth =  new BlueButton20AuthProcessor();
        Processor handleBlueButtonCallback =  new BlueButton20CallbackProcessor();
        Processor setCMSRequestHeaders =  new BlueButton20RequestProcessor();
        Processor setBlueButton20Metadata = new BlueButton20MetadataProcessor();
        Processor convertR3ToR4 = new BlueButton20ResultProcessor();

        restConfiguration()
                .host(blueButtonBaseUri.getHost())
                .port(blueButtonBaseUri.getPort());

        // Blue Button OAuth2 - Authorize route in Blue Button 2.0 & get code
        rest(blueButtonAuthorizeUri.getPath())
                .get()
                .route()
                .routeId(AUTHORIZE_ROUTE_ID)
                .doTry()
                    .process(handleBlueButtonAuth)
                    .toD("${exchangeProperty[location]}")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();

        // Blue Button OAuth2 - Callback to exchange code for token (displayed in the browser)
        rest(blueButtonCallbackUri.getPath())
                .get()
                .route()
                .routeId(CALLBACK_ROUTE_ID)
                .doTry()
                    .process(handleBlueButtonCallback)
                    .to(cmsTokenURL)
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();
        
        // Blue Button 2.0 route - Retrieve patient resources
        rest(blueButtonBaseUri.getPath())
                .get("/{resource}")
                .route()
                .routeId(API_ROUTE_ID)
                .process(setBlueButton20Metadata)
                .doTry()
                    .process(setCMSRequestHeaders)
                    .toD("${exchangeProperty[location]}")
                    .unmarshal().fhirJson("DSTU3")
                    .process(convertR3ToR4)
                    .to("direct:storeandnotify")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();
    }
}
