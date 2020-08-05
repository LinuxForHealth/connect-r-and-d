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
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class BlueButton20RestRouteBuilder extends RouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-rest-authorize";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-rest-callback";
    public final static String API_ROUTE_ID = "bluebutton-20-rest";

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
                .doTry()
                    .process( new BlueButton20AuthProcessor())
                    .toD("${exchangeProperty[location]}")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(LinuxForHealthDirectRouteBuilder.ERROR_CONSUMER_URI)
                .end();

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
                    .to(LinuxForHealthDirectRouteBuilder.ERROR_CONSUMER_URI)
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
                    .to(LinuxForHealthDirectRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(LinuxForHealthDirectRouteBuilder.ERROR_CONSUMER_URI)
                .end();
    }
}
