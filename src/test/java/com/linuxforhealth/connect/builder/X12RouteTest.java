/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import java.util.Properties;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link X12RouteTest}
 */
public class X12RouteTest extends RouteTestSupport {

    private MockEndpoint mockStoreAndNotify;
    private MockEndpoint mockProducer;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new X12RouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.x12.uri", "/x12");
        props.setProperty("lfh.connect.x12.dataformat", "x12");
        props.setProperty("lfh.connect.x12.messagetype", "${header.X12MessageType}");
        props.setProperty("lfh.connect.x12.external.uri", "mock:x12");
        return props;
    }

    /**
     * Overridden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockStoreAndNotify = mockProducerEndpoint(X12RouteBuilder.X12_REST_ROUTE_ID,
                LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                "mock:storeAndNotify");

        mockProducer = MockEndpoint.resolve(context, "mock:x12");

        super.configureContext();
    }

    @Test
    void testX12RouteSingleTransaction() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("x12", "270-005010X279A1.json"));

        mockStoreAndNotify.expectedPropertyReceived("fieldDelimiter", "*");
        mockStoreAndNotify.expectedPropertyReceived("repetitionCharacter", "|");
        mockStoreAndNotify.expectedPropertyReceived("componentSeparator", ":");
        mockStoreAndNotify.expectedPropertyReceived("lineSeparator", "~");
        mockStoreAndNotify.expectedHeaderReceived("X12MessageType", "270");
        mockStoreAndNotify.expectedMessageCount(1);

        mockProducer.expectedMessageCount(1);

        fluentTemplate.to("http://0.0.0.0:8080/x12")
                .withBody(testMessage)
                .send();

        mockStoreAndNotify.assertIsSatisfied();
        mockProducer.assertIsSatisfied();
    }
}
