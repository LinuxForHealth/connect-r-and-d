/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link BlueButton20RouteBuilder#AUTHORIZE_ROUTE_ID}
 */
public class BlueButton20AuthTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder()  {
        return new BlueButton20RouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.bluebutton-20.cms.clientid", "client-id");
        return props;
    }

    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpointById(BlueButton20RouteBuilder.AUTHORIZE_ROUTE_ID,
                BlueButton20RouteBuilder.AUTHORIZE_PRODUCER_ID,
                "mock:result");
        super.configureContext();
        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Validates that the route generates the expected location property for authorization.
      * @throws InterruptedException
     */
    @Test
    void testRoute() throws InterruptedException {
        String expectedLocation = "?args=RAW(https://sandbox.bluebutton.cms.gov/v1/o/authorize/?client_id=client-id&redirect_uri=http://localhost:8080/bluebutton/handler&response_type=code)";

        // executable action differs based on platform
        if (SystemUtils.IS_OS_MAC) {
            expectedLocation = "exec:open" + expectedLocation;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            expectedLocation = "exec:explorer" + expectedLocation;
        } else {
            expectedLocation = "exec:xdg-open" + expectedLocation;
        }

        mockResult.expectedPropertyReceived("location", expectedLocation);

        fluentTemplate.to("{{lfh.connect.bluebutton-20.authorizeuri}}")
                .send();

        mockResult.assertIsSatisfied();
    }
}
