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
import java.util.UUID;

/**
 * Tests {@link X12RouteTest}
 */
public class X12RouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new X12RouteBuilder();
    }

    /**
     * Overridden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpoint(X12RouteBuilder.ROUTE_ID,
                LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testRoute() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("x12", "270-005010X279A1.json"))
                .replace(System.lineSeparator(), "");

        String expectedRequest = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedRequest);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:X12_X12?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "X12");
        mockResult.expectedPropertyReceived("messageType", "X12");
        mockResult.expectedPropertyReceived("routeId", "x12");

        fluentTemplate.to("http://0.0.0.0:8080/x12")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();

        String expectedRouteUri = "jetty:http://0.0.0.0:8080/x12?httpMethodRestrict=POST";
        String actualRouteUri = mockResult.getExchanges().get(0).getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Exchange mockExchange = mockResult.getExchanges().get(0);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
