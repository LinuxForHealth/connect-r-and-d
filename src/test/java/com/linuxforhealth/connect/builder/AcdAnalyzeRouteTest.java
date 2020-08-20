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

import java.util.UUID;

/**
 * Tests {@link AcdAnalyzeRouteBuilder#ACD_ANALYZE_ROUTE_ID}
 */
public class AcdAnalyzeRouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new AcdAnalyzeRouteBuilder();
    }

    /**
     * Configures mock responses and endpoints for route testing
     * @throws Exception
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        setProducerResponse(AcdAnalyzeRouteBuilder.ACD_ANALYZE_ROUTE_ID,
                AcdAnalyzeRouteBuilder.ACD_ANALYZE_REQUEST_PRODUCER_ID,
                "acd",
                "insights-response.json");

        mockProducerEndpointById(AcdAnalyzeRouteBuilder.ACD_ANALYZE_ROUTE_ID,
                AcdAnalyzeRouteBuilder.ACD_ANALYZE_PRODUCER_ID,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testRoute() throws Exception {

        mockResult.expectedMessageCount(1);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:ACD_INSIGHTS?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "ACD");
        mockResult.expectedPropertyReceived("messageType", "INSIGHTS");

        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("acd", "insights-request.txt"));


        fluentTemplate.to(AcdAnalyzeRouteBuilder.ACD_ANALYZE_CONSUMER_URI)
                .withHeader("content-type", "application/json")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();

        Exchange mockExchange = mockResult.getExchanges().get(0);
        String expectedRouteUri = "direct://acd-analyze";
        String actualRouteUri = mockExchange.getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());

    }
}
