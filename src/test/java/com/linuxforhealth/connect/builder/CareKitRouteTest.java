/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LinuxForHealthAssertions;
import com.linuxforhealth.connect.support.TestUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CareKitRouteBuilder}
 */
public class CareKitRouteTest extends RouteTestSupport {

    private MockEndpoint mockStoreAndNotify;
    private MockEndpoint mockExternalEndpoint;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new CareKitRouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.carekit.uri", "/carekit");
        props.setProperty("lfh.connect.carekit.dataformat", "carekit");
        props.setProperty("lfh.connect.carekit.messagetype", "${header.resource}");
        props.setProperty("lfh.connect.carekit.external.uri", "mock:carekit");
        return props;
    }

    /**
     * Overridden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockStoreAndNotify = mockProducerEndpoint(CareKitRouteBuilder.ROUTE_ID,
                                        LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                                        "mock:storeAndNotify");

        mockExternalEndpoint = mockProducerEndpoint(CareKitRouteBuilder.ROUTE_ID,
            LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
            "mock:carekit");

        super.configureContext();

    }

    @Test
    void testRoute() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("carekit", "patient.json"))
                .replace(System.lineSeparator(), "");

        String expectedMessage = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockStoreAndNotify.expectedMessageCount(1);
        mockStoreAndNotify.expectedBodiesReceived(expectedMessage);
        mockStoreAndNotify.expectedPropertyReceived("dataStoreUri", "kafka:CAREKIT_PATIENT?brokers=localhost:9094");
        mockStoreAndNotify.expectedPropertyReceived("dataFormat", "CAREKIT");
        mockStoreAndNotify.expectedPropertyReceived("messageType", "PATIENT");
        mockStoreAndNotify.expectedPropertyReceived("routeId", "carekit");

        mockExternalEndpoint.expectedMessageCount(1);
        mockExternalEndpoint.expectedBodiesReceived(expectedMessage);

        fluentTemplate.to("http://0.0.0.0:8080/carekit/patient")
                .withBody(testMessage)
                .send();

        mockStoreAndNotify.assertIsSatisfied();

        String expectedRouteUri = "jetty:http://0.0.0.0:8080/carekit/patient?httpMethodRestrict=POST";
        String actualRouteUri = mockStoreAndNotify.getExchanges().get(0).getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Exchange mockExchange = mockStoreAndNotify.getExchanges().get(0);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
