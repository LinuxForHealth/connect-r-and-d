/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link BaseRouteBuilder} with a default implementation
 */
public class BaseRouteTest extends CamelTestSupport {

    private MockEndpoint mockResult;

    /**
     * Sets properties for the unit test
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        props.setProperty("lfh.connect.default.uri", "direct:start");
        props.setProperty("lfh.connect.default.dataformat", "csv");
        props.setProperty("lfh.connect.default.messagetype", "person");
        return props;
    }

    @Override
    protected RoutesBuilder createRouteBuilder()  {
        return new BaseRouteBuilder() {
            @Override
            protected String getRoutePropertyNamespace() {
                return "lfh.connect.default";
            }

            @Override
            protected void buildRoute(String routePropertyNamespace) {
                from("{{lfh.connect.default.uri}}")
                .to("mock:result");
            }
        };
    }

    @BeforeEach
    void setupFixtures() {
        mockResult = MockEndpoint.resolve(context, "mock:result");
        fluentTemplate.start();
    }

    @Test
    void testRoute() throws InterruptedException{
        fluentTemplate.to("direct:start")
                .withBody("test")
                .send();
        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived("test");
        mockResult.assertIsSatisfied();
    }
}
