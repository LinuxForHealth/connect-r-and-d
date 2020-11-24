/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Tests {@link LinuxForHealthRouteBuilder#NOTIFY_CONSUMER_URI}
 */
public class LinuxForHealthNotifyTest extends RouteTestSupport {

    private MockEndpoint mockMessagingResult;

    /**
     * Provides properties to support mocking data and messaging components.
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.test.uri", "direct:test-notify");
        props.setProperty("lfh.connect.test.dataformat", "csv");
        props.setProperty("lfh.connect.test.messagetype", "person");

        props.setProperty("lfh.connect.messaging.response.uri", "mock:messaging");
        props.setProperty("lfh.connect.messaging.error.uri", "mock:error-messaging");
        props.setProperty("lfh.connect.datastore.remote-events.consumer.uri", "direct:remote-events");
        return props;
    }

    /**
     * Creates routes for unit tests.
     * @return {@link org.apache.camel.builder.RouteBuilder}
     */
    @Override
    protected RoutesBuilder[] createRouteBuilders() {
        return new RoutesBuilder[]{
                new LinuxForHealthRouteBuilder(),
                new BaseRouteBuilder() {
                    @Override
                    protected String getRoutePropertyNamespace() { return "lfh.connect.test"; }
                    @Override
                    protected void buildRoute(String routePropertyNamespace) {
                        from("{{lfh.connect.test.uri}}")
                        .routeId("test-notify")
                        .process(exchange -> {
                            exchange.setProperty("uuid", "312e02e6-9a33-494a-8928-1aba9dbedf9c");
                            exchange.setProperty("routeUri", "http://0.0.0.0:8080/fhir/r4/PATIENT");
                            exchange.setProperty("dataFormat", "fhir-r4");
                            exchange.setProperty("timestamp", 1596830090);
                            exchange.setProperty("dataStoreUri", "kafka:FHIR_R4_PATIENT?brokers=localhost:9094");
                            exchange.setProperty("status", "success");

                            RecordMetadata rm =  new RecordMetadata(
                                    new TopicPartition("FHIR-R4_PATIENT", 0), 0, 0, 1591732928186L, 0L, 0, 0);
                            List<RecordMetadata> metaRecords = new ArrayList<>();
                            metaRecords.add(rm);
                            exchange.getIn().setHeader(KafkaConstants.KAFKA_RECORDMETA, metaRecords);

                            // Base64-encode the body because we're returning it in the message
                            String exchangeBody = exchange.getIn().getBody(String.class);
                            String result = Base64.getEncoder().encodeToString(exchangeBody.getBytes(StandardCharsets.UTF_8));

                            // Create the message envelope, which we generally do as a part of the data store step
                            LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
                            msg.setData(result);
                            exchange.getIn().setBody(msg.toString());
                        })
                        .to(LinuxForHealthRouteBuilder.NOTIFY_CONSUMER_URI);
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
        super.configureContext();
        mockMessagingResult = MockEndpoint.resolve(context, "mock:messaging");
    }

    @Test
    void testNotifyRoute() throws Exception {
        String expectedMsg = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("lfh", "notify.json"));
        mockMessagingResult.expectedMessageCount(1);
        mockMessagingResult.expectedBodiesReceived(expectedMsg);

        fluentTemplate.to("direct:test-notify")
                .withBody("notify test message")
                .send();
        mockMessagingResult.assertIsSatisfied();
    }
}
