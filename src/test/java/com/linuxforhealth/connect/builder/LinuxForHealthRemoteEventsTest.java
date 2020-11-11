/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#REMOTE_EVENTS_ROUTE_ID}
 */
public class LinuxForHealthRemoteEventsTest extends RouteTestSupport {

    private MockEndpoint mockRemoteEventsResult;
    private ProducerTemplate mockKafkaProducer;

    /**
     * Provides properties to support mocking data and messaging components.
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-notify");
        props.setProperty("lfh.connect.messaging.response.uri", "mock:messaging");
        props.setProperty("lfh.connect.messaging.error.uri", "mock:error-messaging");
        props.setProperty("lfh.connect.datastore.remote-events.consumer.uri", "direct:remote-events");
        return props;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new LinuxForHealthRouteBuilder();
    }

    /**
     * Overriden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpointById(LinuxForHealthRouteBuilder.REMOTE_EVENTS_ROUTE_ID,
                LinuxForHealthRouteBuilder.REMOTE_EVENTS_PRODUCER_ID,
                "mock:remote-events-result");

        context.getRegistry().bind("LFHKafkaConsumer", new LFHKafkaConsumer());
        super.configureContext();

        mockConsumer(LinuxForHealthRouteBuilder.REMOTE_EVENTS_ROUTE_ID, "direct:remote-events");
        mockKafkaProducer = context.createProducerTemplate();
        mockRemoteEventsResult = MockEndpoint.resolve(context, "mock:remote-events-result");
    }

    /**
     * Tests {@link LinuxForHealthRouteBuilder#REMOTE_EVENTS_ROUTE_ID}
     * @throws Exception
     */
    @Test
    void testRoute() throws Exception {

        String expectedMsg = "mock-image-bytes";
        expectedMsg = Base64.getEncoder().encodeToString(expectedMsg.getBytes(StandardCharsets.UTF_8));

        String inputMsg = context
            .getTypeConverter()
            .convertTo(String.class, TestUtils.getMessage("lfh", "remote-events-producer-input.json"));

        mockKafkaProducer.sendBody("direct:remote-events", inputMsg);

        mockRemoteEventsResult.expectedMessageCount(1);
        mockRemoteEventsResult.expectedBodiesReceived(expectedMsg);
        mockRemoteEventsResult.expectedPropertyReceived("dataStoreUri", "kafka:DICOM_IMAGE?brokers=kafka:9092");
        mockRemoteEventsResult.expectedPropertyReceived("routeUri", "jetty:http://0.0.0.0:9090/orthanc/instances?httpMethodRestrict=POST");
        mockRemoteEventsResult.expectedPropertyReceived("routeId", "orthanc-post");
        mockRemoteEventsResult.expectedPropertyReceived("messageType", "IMAGE");
        mockRemoteEventsResult.expectedPropertyReceived("dataFormat", "DICOM");
        mockRemoteEventsResult.expectedPropertyReceived("timestamp", 1598619641);
        mockRemoteEventsResult.expectedPropertyReceived("success", "success");
        mockRemoteEventsResult.assertIsSatisfied();
    }
}
