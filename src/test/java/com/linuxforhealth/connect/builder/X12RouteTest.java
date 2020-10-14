/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import com.linuxforhealth.connect.support.x12.X12TransactionSplitter;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
        context.getRegistry().bind("x12splitter", new X12TransactionSplitter());

        mockProducerEndpoint(X12RouteBuilder.ROUTE_ID,
                LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testX12RouteSingleTransaction() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("x12", "270-005010X279A1.json"));

        mockResult.expectedMessageCount(1);

        fluentTemplate.to("http://0.0.0.0:8080/x12")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();
    }

    @Test
    void testX12RouteMultipleTransaction() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("x12", "270-837-005010X279A1.json"));

        mockResult.expectedMessageCount(3);

        fluentTemplate.to("http://0.0.0.0:8080/x12")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();
    }
}
