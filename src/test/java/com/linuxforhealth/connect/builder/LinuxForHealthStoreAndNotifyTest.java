/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#STORE_AND_NOTIFY_CONSUMER_URI}
 */
public class LinuxForHealthStoreAndNotifyTest extends RouteTestSupport {

    private MockEndpoint mockDataStoreResult;
    private MockEndpoint mockMessagingResult;

    /**
     * Provides properties to support mocking data and messaging components.
     *
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-store-notify");
        props.setProperty("lfh.connect.test.dataformat", "csv");
        props.setProperty("lfh.connect.test.messagetype", "person");

        props.setProperty("lfh.connect.datastore.uri", "mock:data-store");
        props.setProperty("lfh.connect.messaging.uri", "mock:messaging");
        return props;
    }

    /**
     * Creates routes for unit tests.
     *
     * @return {@link org.apache.camel.builder.RouteBuilder}
     */
    @Override
    protected RoutesBuilder[] createRouteBuilders() {
        return new RoutesBuilder[]{
                new LinuxForHealthRouteBuilder(),
                new BaseRouteBuilder() {
                    @Override
                    protected String getRoutePropertyNamespace() {
                        return "lfh.connect.test";
                    }

                    @Override
                    protected void buildRoute(String routePropertyNamespace) {
                        from("{{lfh.connect.test.uri}}")
                                .routeId("test-store-notify")
                                .process(exchange -> {
                                    String dataStoreUri = SimpleBuilder
                                            .simple("${properties:lfh.connect.datastore.uri}")
                                            .evaluate(exchange, String.class);
                                    exchange.setProperty("dataStoreUri", dataStoreUri);
                                })
                                .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
                    }
                }
        };
    }

    /**
     * Configures mock endpoints
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        super.configureContext();
        mockDataStoreResult = MockEndpoint.resolve(context, "mock:data-store");
        mockMessagingResult = MockEndpoint.resolve(context, "mock:messaging");
    }

    @Test
    void testStoreRoute() throws Exception {
        mockDataStoreResult.expectedMessageCount(1);
        mockMessagingResult.expectedMessageCount(1);

        fluentTemplate.to("direct:test-store-notify")
                .withBody("test message")
                .send();

        mockDataStoreResult.assertIsSatisfied();
        mockMessagingResult.assertIsSatisfied();
    }
}
