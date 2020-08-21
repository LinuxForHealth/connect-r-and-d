/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * Tests {@link MetaDataProcessor}
 */
class MetaDataProcessorTest extends CamelTestSupport {

    private MockEndpoint mockResult;
    private ProducerTemplate producerTemplate;

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        props.setProperty("lfh.connect.datastore.uri", "kafka:<topicName>?brokers=localhost:9094");
        props.setProperty("lfh.connect.meta.uri", "direct:start");
        props.setProperty("lfh.connect.meta.dataformat", "csv");
        props.setProperty("lfh.connect.meta.messagetype", "person");
        return props;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("{{lfh.connect.meta.uri}}")
                .routeId("meta-data-processor-test")
                .process(new MetaDataProcessor("lfh.connect.meta"))
                .to("mock:result");
            }
        };
    }

    @BeforeEach
    void configureFixtures() {
        producerTemplate = new DefaultProducerTemplate(context);
        producerTemplate.start();
        mockResult = MockEndpoint.resolve(context, "mock:result");

    }

    @Test
    void testProcess() throws InterruptedException {
        String csvData = "1,Oscar,Whitmire\n2,Emmie,Whitmire";
        String expectedData = Base64.getEncoder().encodeToString(csvData.getBytes(StandardCharsets.UTF_8));

        producerTemplate.sendBody("direct:start", csvData);

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedData);
        mockResult.expectedPropertyReceived("routeId", "meta-data-processor-test");
        mockResult.expectedPropertyReceived("routeUri", "direct:start");
        mockResult.expectedPropertyReceived("dataFormat", "CSV");
        mockResult.expectedPropertyReceived("messageType", "PERSON");
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:CSV_PERSON?brokers=localhost:9094");

        mockResult.assertIsSatisfied();

        // validate generated fields
        Exchange mockExchange = mockResult.getExchanges().get(0);
        UUID actualUUID = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUUID.toString().length());

        Long timestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(timestamp);
        Assertions.assertTrue(timestamp > 0);
    }
}
