/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#STORE_CONSUMER_URI}
 */
public class LinuxForHealthStoreTest extends RouteTestSupport {

    private MockEndpoint mockDataStoreResult;

    /**
     * Provides properties to support mocking data and messaging components.
     *
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-store");
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
                                .routeId("test-store")
                                .process(exchange -> {
                                    exchange.setProperty("uuid", "312e02e6-9a33-494a-8928-1aba9dbedf9c");
                                    exchange.setProperty("routeUri", "http://0.0.0.0:8080/fhir/r4/PATIENT");
                                    exchange.setProperty("dataFormat", "fhir-r4");
                                    exchange.setProperty("timestamp", 1596830090);
                                    exchange.setProperty("status", "success");

                                    String dataStoreUri = SimpleBuilder
                                            .simple("${properties:lfh.connect.datastore.uri}")
                                            .evaluate(exchange, String.class);
                                    exchange.setProperty("dataStoreUri", dataStoreUri);

                                    RecordMetadata rm = new RecordMetadata(
                                            new TopicPartition("FHIR-R4_PATIENT", 0), 0, 0, 1591732928186L, 0L, 0, 0);
                                    List<RecordMetadata> metaRecords = new ArrayList<>();
                                    metaRecords.add(rm);
                                    exchange.getIn().setHeader(KafkaConstants.KAFKA_RECORDMETA, metaRecords);
                                })
                                .to(LinuxForHealthRouteBuilder.STORE_CONSUMER_URI);
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
    }

    @Test
    void testStoreRoute() throws Exception {
        String expectedMsg = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("lfh", "store.json"));

        mockDataStoreResult.expectedBodiesReceived(expectedMsg);
        mockDataStoreResult.expectedMessageCount(1);

        String inputMsg = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("fhir", "fhir-r4-patient-bundle.json"));

        fluentTemplate.to("direct:test-store")
                .withBody(inputMsg)
                .send();

        mockDataStoreResult.assertIsSatisfied();
    }
}
