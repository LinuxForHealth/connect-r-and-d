/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#ERROR_CONSUMER_URI}
 */
public class LinuxForHealthErrorTest extends RouteTestSupport {

    private MockEndpoint mockDataStoreResult;
    private MockEndpoint mockMessagingResult;
    private MockEndpoint mockUnreachableResult;

    /**
     * Provides properties to support mocking data and messaging components.
     *
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-error");
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
                        // define a route which will throw an exception, triggering the error route
                        from("{{lfh.connect.test.uri}}")
                                .routeId("test-error-handler")
                                .throwException(RuntimeException.class, "runtime exception")
                                .to("mock:test-error");
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
        mockUnreachableResult = MockEndpoint.resolve(context, "mock:test-error");
    }

    /**
     * Tests {@link LinuxForHealthRouteBuilder#ERROR_CONSUMER_URI}
     *
     * @throws Exception
     */
    @Test
    void testErrorRoute() throws Exception {
        mockUnreachableResult.expectedMessageCount(0);
        mockDataStoreResult.expectedMessageCount(0);

        String expectedMsg = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("lfh", "error.json"));

        mockMessagingResult.expectedBodiesReceived(expectedMsg);
        mockMessagingResult.expectedMessageCount(1);

        fluentTemplate.to("direct:test-error")
                .withBody("1,Donald,Duck")
                .send();

        mockUnreachableResult.assertIsSatisfied();
        mockDataStoreResult.assertIsSatisfied();
        mockMessagingResult.assertIsSatisfied();
    }
}
