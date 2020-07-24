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
import java.net.URI;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.PropertiesComponent;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class BlueButton20RestRouteBuilder extends RouteBuilder {

    public final static String AUTHORIZE_ROUTE_ID = "bluebutton-20-rest-authorize";
    public final static String CALLBACK_ROUTE_ID = "bluebutton-20-rest-callback";
    public final static String API_ROUTE_ID = "bluebutton-20-rest";

    @Override
    public void configure() {

        PropertiesComponent contextProperties = getContext().getPropertiesComponent();

        URI blueButtonUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.fhir_r4_rest.uri")
                .orElseThrow(() -> new RuntimeException("property lfh.connect.fhir_r4_rest.uri not found")));

        URI blueButtonAuthorizeUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.authorizeUri")
                .orElseThrow(() -> new RuntimeException("property lfh.connect.bluebutton_20_rest.authorizeUri not found")));

        URI blueButtonCallbackUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.callbackUri")
                .orElseThrow(() -> new RuntimeException("property lfh.connect.bluebutton_20_rest.callbackUri not found")));

        URI cmsTokenURL = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.tokenUri")
                .orElseThrow(() -> new RuntimeException("property lfh.connect.bluebutton_20_rest.tokenUri not found")));

        restConfiguration()
                .host(blueButtonUri.getHost())
                .port(blueButtonUri.getPort());

        // Blue Button OAuth2 - Authorize route in Blue Button 2.0 & get code
        rest(blueButtonAuthorizeUri.getPath())
                .get()
                .route()
                .routeId(AUTHORIZE_ROUTE_ID)
                .doTry()
                    .process( new BlueButton20AuthProcessor())
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
                    .process(new BlueButton20CallbackProcessor())
                    .to(cmsTokenURL.toString())
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
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
                    .to("direct:storeandnotify")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();
    }
}
