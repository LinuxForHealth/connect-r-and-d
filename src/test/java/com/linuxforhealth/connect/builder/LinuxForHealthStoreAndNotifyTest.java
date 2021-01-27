/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#STORE_AND_NOTIFY_CONSUMER_URI}
 */
public class LinuxForHealthStoreAndNotifyTest extends RouteTestSupport {

    private final Logger logger = LoggerFactory.getLogger(LinuxForHealthStoreAndNotifyTest.class);

    private MockEndpoint mockDataStoreResult;
    private MockEndpoint mockMessagingResult;
    private MockEndpoint mockLocationHeaderResult;

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
        props.setProperty("lfh.connect.messaging.response.uri", "mock:messaging");
        props.setProperty("lfh.connect.messaging.error.uri", "mock:error-messaging");
        props.setProperty("lfh.connect.datastore.remote-events.consumer.uri", "direct:remote-events");
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
        context.getRegistry().bind("LFHKafkaConsumer", new LFHKafkaConsumer());

        mockProducerEndpointById(
                LinuxForHealthRouteBuilder.LOCATION_HEADER_ID,
                LinuxForHealthRouteBuilder.LOCATION_HEADER_PRODUCER_ID,
                "mock:result"
        );

        super.configureContext();
        mockDataStoreResult = MockEndpoint.resolve(context, "mock:data-store");
        mockMessagingResult = MockEndpoint.resolve(context, "mock:messaging");
        mockLocationHeaderResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testLocationResponseHeader() throws Exception {

        // List<RecordMetadata> exchange header
        RecordMetadata recordMetadata =  new RecordMetadata(
                new TopicPartition("FHIR-R4_PATIENT", 0), 0, 0,
                    1591732928186L, 0L, 0, 0);
        List<RecordMetadata> recordMetadataList = new ArrayList<>();
        recordMetadataList.add(recordMetadata);

        // Validate correctness of generated LFH location header
        mockLocationHeaderResult.expectedHeaderReceived(LinuxForHealthRouteBuilder.LFH_LOCATION_HEADER,
                "/datastore/message?topic=FHIR-R4_PATIENT&partition=0&offset=0");
        mockLocationHeaderResult.expectedMessageCount(1);

        fluentTemplate.to("direct:location-header")
                .withHeader(KafkaConstants.KAFKA_RECORDMETA, recordMetadataList)
                .send();

        mockLocationHeaderResult.assertIsSatisfied();
    }

    @Test
    void testLocationResponseHeaderMissingRecordMetadata() throws Exception {

        // Make sure we do not get a valid LFH header in this instance, but the route call proceeds w/out error
        mockLocationHeaderResult.expectedHeaderReceived(LinuxForHealthRouteBuilder.LFH_LOCATION_HEADER, null);
        mockLocationHeaderResult.expectedMessageCount(1);

        fluentTemplate.to("direct:location-header")
                .send();

        mockLocationHeaderResult.assertIsSatisfied();
    }

    //@Test
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
