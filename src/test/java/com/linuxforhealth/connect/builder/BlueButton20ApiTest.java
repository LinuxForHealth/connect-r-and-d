/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LinuxForHealthAssertions;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * Tests {@link BlueButton20RouteBuilder#API_ROUTE_ID}
 */
public class BlueButton20ApiTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder()  {
        return new BlueButton20RouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.bluebutton_20.cms.clientid", "client-id");
        props.setProperty("lfh.connect.bluebutton_20.cms.clientsecret", "client-secret");
        return props;
    }

    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        setProducerResponse(BlueButton20RouteBuilder.API_ROUTE_ID,
                BlueButton20RouteBuilder.API_ROUTE_BLUE_BUTTON_REQUEST_PRODUCER_ID,
                "bluebutton-20",
                "patient-result.json");

        mockProducerEndpointById(BlueButton20RouteBuilder.API_ROUTE_ID,
                BlueButton20RouteBuilder.API_ROUTE_PRODUCER_ID,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }


    @Test
    void testRoute() throws Exception {

        String expectedMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("bluebutton-20", "patient-result.json"));

        expectedMessage = Base64.getEncoder().encodeToString(expectedMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:FHIR-R4_PATIENT?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "FHIR-R4");
        mockResult.expectedPropertyReceived("messageType", "PATIENT");
        mockResult.expectedPropertyReceived("routeId", "bluebutton-20");


        fluentTemplate.to("{{lfh.connect.bluebutton-20.uri}}/Patient?-19990000002154")
                .withHeader("Authorization", "387Rf1c2JTuduYuOQIuRHUlVOvJsib")
                .request();

        mockResult.assertIsSatisfied();

        Exchange mockExchange = mockResult.getExchanges().get(0);

        String expectedRouteUri = "http://0.0.0.0:8080/bluebutton/v1/PATIENT?-19990000002154=";
        String actualRouteUri = mockExchange.getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);


        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
