/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.util.Properties;

import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import com.linuxforhealth.connect.support.TestUtils;
import com.linuxforhealth.connect.support.etl.PractitionerCsvFormat;
import com.linuxforhealth.connect.support.etl.PractitionerCsvTransform;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * Tests {@link EtlRouteBuilder}
 */
@TestInstance(Lifecycle.PER_CLASS)
public class EtlRouteTest extends RouteTestSupport{

    private MockEndpoint mockError;
    private MockEndpoint mockPersist;
    private String testMessage;

    /**
     * Updates LinuxForHealth Route Properties to use mock endpoints.
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();

        props.setProperty("lfh.connect.datastore.uri", "mock:data-store");
        props.setProperty("lfh.connect.messaging.response.uri", "mock:messaging");
        props.setProperty("lfh.connect.messaging.error.uri", "mock:error-messaging");
        return props;
    }


    /**
     * Creates the {@link EtlRouteBuilder} and {@link LinuxForHealthRouteBuilder} implementations
     */
    @Override
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
            return new RoutesBuilder[] {
                new EtlRouteBuilder(),
                new LinuxForHealthRouteBuilder()
            };
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        context.getRegistry().bind("LFHKafkaConsumer", new LFHKafkaConsumer());
        context.getRegistry().bind("practitionercsvformat", new PractitionerCsvFormat());
        context.getRegistry().bind("practitionercsvtransform", new PractitionerCsvTransform());
        return context;
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        super.beforeTestExecution(context);
        if (mockError == null) {
            mockError = mockProducerEndpointById(LinuxForHealthRouteBuilder.ERROR_ROUTE_ID,
                LinuxForHealthRouteBuilder.ERROR_PRODUCER_ID,
                "mock:error");
        }

        if (mockPersist == null) {
            mockPersist = mockProducerEndpointById(EtlRouteBuilder.ROUTE_ID,
                EtlRouteBuilder.DATA_PERSIST_PRODUCER_ID,
                "mock:dataPersist");
        }
    }

    /**
     * Loads the ETL test message (source data message)
     * @throws Exception
     */
    @BeforeEach
    void loadTestMessage() throws Exception {
        testMessage = context
                      .getTypeConverter()
                      .convertTo(String.class, TestUtils.getMessage("etl", "practitioner.csv"));
    }


    /**
     * Tests the ETLRoute with an invalid ETLMessageType header.
     * @throws Exception
     */
    @Test
    void testRouteInvalidMessageType() throws Exception {
        mockError.expectedMessageCount(1);
        mockPersist.expectedMessageCount(0);
        
        fluentTemplate.to("http://0.0.0.0:8080/etl")
                      .withBody(testMessage)
                      .withHeader("ETLMessageType", "pracitioner?csv")
                      .withHeader("Content-Type", "text/csv")
                      .send();

        mockError.assertIsSatisfied();
        mockPersist.assertIsSatisfied();
    }

    /**
     * Tests the ETLRoute with an invalid Content-Type header.
     * @throws Exception
     */
    @Test
    void testRouteInvalidContentType() throws Exception {
        mockError.expectedMessageCount(1);
        mockPersist.expectedMessageCount(0);
        
        fluentTemplate.to("http://0.0.0.0:8080/etl")
                      .withBody(testMessage)
                      .withHeader("ETLMessageType", "practitioner_csv")
                      .withHeader("Content-Type", "text/json")
                      .send();

        mockError.assertIsSatisfied();
        mockPersist.assertIsSatisfied();
    }

    /**
     * Tests the ETLRoute with valid parameters.
     * @throws Exception
     */
    @Test
    void testRoute() throws Exception {
        mockError.expectedMessageCount(0);

        mockPersist.expectedMessageCount(1);
        mockPersist.expectedPropertyReceived("bindyDataFormat", "bindy-csv");
        mockPersist.expectedPropertyReceived("bindyType", "Csv");
        mockPersist.expectedPropertyReceived("bindyBean", "com.linuxforhealth.connect.support.etl.PractitionerCsvFormat");
        mockPersist.expectedPropertyReceived("transformBean", "practitionercsvtransform");

        fluentTemplate.to("http://0.0.0.0:8080/etl")
                      .withBody(testMessage)
                      .withHeader("ETLMessageType", "practitioner_csv")
                      .withHeader("Content-Type", "text/csv;charset=UTF-8")
                      .send();

        mockError.assertIsSatisfied();
        mockPersist.assertIsSatisfied();
    }
}
