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
import org.apache.camel.Processor;
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

        URI blueButtonBaseUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.fhir_r4_rest.uri")
                .orElse("http://0.0.0.0:8080/fhir/r4"));

        URI blueButtonAuthorizeUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.authorizeUri")
                .orElse("http://0.0.0.0:8080/bluebutton/authorize"));

        URI blueButtonCallbackUri = URI.create(
                contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.callbackUri")
                .orElse("http://localhost:8080/bluebutton/handler"));

        String cmsTokenURL = contextProperties
                .resolveProperty("lfh.connect.bluebutton_20_rest.tokenUri")
                .orElse("lfh.connect.bluebutton_20.cmsTokenUri");

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
