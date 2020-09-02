/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Tests {@link BlueButton20RouteBuilder#CALLBACK_ROUTE_ID}
 */
public class BlueButton20CallbackTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder()  {
        return new BlueButton20RouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.bluebutton-20.cms.tokenUri", "https://sandbox.bluebutton.cms.gov/v1/o/token/");
        props.setProperty("lfh.connect.bluebutton-20.cms.clientid", "client-id");
        props.setProperty("lfh.connect.bluebutton-20.cms.clientsecret", "client-secret");
        return props;
    }

    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpointById(BlueButton20RouteBuilder.CALLBACK_ROUTE_ID,
                BlueButton20RouteBuilder.CALLBACK_PRODUCER_ID,
                "mock:result");
        super.configureContext();
        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Validates that Blue Button 20 Callback exchange headers and body are properly formed.
      * @throws InterruptedException
     */
    @Test
    void testRoute() throws InterruptedException {

        mockResult.expectedMessageCount(1);

        String expectedBody = "code=auth-code&grant_type=authorization_code";
        mockResult.expectedBodiesReceived(expectedBody);

        mockResult.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        String expectedAuthHeader = "Basic " + Base64.getEncoder().encodeToString("client-id:client-secret".getBytes(StandardCharsets.UTF_8));
        mockResult.expectedHeaderReceived("Authorization", expectedAuthHeader);

        mockResult.expectedHeaderReceived("Content-Type", "application/x-www-form-urlencoded");
        mockResult.expectedHeaderReceived("Content-Length", expectedBody.length());

        fluentTemplate.to("{{lfh.connect.bluebutton-20.handleruri}}")
                .withHeader("code", "auth-code")
                .send();

        mockResult.assertIsSatisfied();
    }
}
