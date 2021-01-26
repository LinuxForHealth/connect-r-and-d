/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#REMOTE_EVENTS_ROUTE_ID}
 */
public class LinuxForHealthExternalFhirServersTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    /**
     * Provides properties to support mocking data and messaging components.
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-notify");
        props.setProperty("lfh.connect.messaging.response.uri", "mock:messaging");
        props.setProperty("lfh.connect.datastore.remote-events.consumer.uri", "direct:remote-events");
        props.setProperty("lfh.connect.fhir-r4.externalservers", "http://localhost:9081/fhir-server/api/v4");
        return props;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new FhirR4RouteBuilder();
    }

    /**
     * Overriden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {

        setProducerResponseByToString(FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_ID,
                "DynamicTo*",
                "fhir",
                "ext-fhir-server-mock-result.json");

        addLast(FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_ID, "mock:result");
    
        context.getRegistry().bind("LFHKafkaConsumer", new LFHKafkaConsumer());
        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Tests {@link LinuxForHealthRouteBuilder#REMOTE_EVENTS_ROUTE_ID}
     * @throws Exception
     */
    @Test
    void testRoute() throws Exception {

        // Get the string that represents the result of the fhir-r4 route
        String inputMessage = context
            .getTypeConverter()
            .convertTo(String.class, TestUtils.getMessage("fhir", "ext-fhir-server-route-input.json"))
            .replace(System.lineSeparator(), "");

        String expectedMessage = context
            .getTypeConverter()
            .convertTo(String.class, TestUtils.getMessage("fhir", "ext-fhir-server-mock-result.json"));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        mockResult.expectedPropertyReceived("result", inputMessage);
        mockResult.expectedHeaderReceived("Prefer", "return=OperationOutcome");
        mockResult.expectedHeaderReceived("recipientList", "http://localhost:9081/fhir-server/api/v4/Patient");

        fluentTemplate.to(FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_URI)
            .withBody(inputMessage)
            .withHeader("resource", "Patient")
            .send();

        mockResult.assertIsSatisfied();
    }
}
