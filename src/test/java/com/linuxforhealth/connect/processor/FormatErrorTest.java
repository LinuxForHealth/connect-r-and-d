/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link FormatErrorProcessor} processor
 */
public class FormatErrorTest extends CamelTestSupport {

    private Exchange mockedExchange;
    private FormatErrorProcessor formatError;

    private Exchange createMockExchange() {
        Exchange mockedExchange = new DefaultExchange(context);
        mockedExchange.getIn().setHeader("timestamp", 1592514822);
        mockedExchange.adapt(ExtendedExchange.class).setFromRouteId("hl7-v2-mllp");
        mockedExchange.getIn().setHeader("routeUrl", "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder");
        mockedExchange.getIn().setHeader("dataStoreUrl", "kafka:HL7v2_ADT?brokers=localhost:9092");
        mockedExchange.getIn().setHeader("dataFormat", "hl7-v2");
        mockedExchange.getIn().setHeader("uuid", "123e4567-e89b-42d3-a456-556642441234");
        Exception ex = new Exception("An error occurred");
        mockedExchange.setProperty(Exchange.EXCEPTION_CAUGHT, ex);

        return mockedExchange;
    }

    /**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() {
        mockedExchange = createMockExchange();
        formatError = new FormatErrorProcessor();
    }

    /**
     * Tests {@link FormatErrorProcessor#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() {
        formatError.process(mockedExchange);
        String expectedBody = "{\"meta\":{\"routeId\":\"hl7-v2-mllp\","+
            "\"uuid\":\"123e4567-e89b-42d3-a456-556642441234\","+
            "\"routeUrl\":\"netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder\","+
            "\"dataFormat\":\"hl7-v2\",\"timestamp\":1592514822,"+
            "\"dataStoreUrl\":\"kafka:HL7v2_ADT?brokers=localhost:9092\","+
            "\"status\":\"error\"},\"data\":\"An error occurred\"}";
        String actualBody = mockedExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedBody, actualBody);
    }
}
