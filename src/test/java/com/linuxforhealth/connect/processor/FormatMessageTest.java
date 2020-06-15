/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link FormatMessageProcessor} processor
 */
public class FormatMessageTest extends CamelTestSupport {

    private Exchange mockedExchange;
    private FormatMessageProcessor formatNotification;

    private Exchange createMockExchange() {

        mockedExchange.getIn().setHeader("routeUrl", "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder");
        mockedExchange.getIn().setHeader("dataStoreUrl", "kafka:HL7v2_ADT?brokers=localhost:9092");
        mockedExchange.getIn().setHeader("dataFormat", "hl7-v2");
        mockedExchange.getIn().setHeader("uuid", "ID-MBP-2-attlocal-net-1592229483323-2-1");

        return mockedExchange;
    }

    /**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() {
        mockedExchange = createMockExchange();
        formatMessage = new FormatMessageProcessor();
    }

    /**
     * Tests {@link FormatMessageProcessor#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() {
        formatMessage.process(mockedExchange);
        String expectedBody = "{\"meta\":{"+
        "\"routeUrl\":\"netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder\","+
        "\"dataFormat\":\"hl7-v2\",\"dataRecordLocation\":[\"HL7v2_ADT-0@0\"],"+
        "\"uuid\":\"ID-MBP-2-attlocal-net-1592229483323-2-1\","+
        "\"dataStoreUrl\":\"kafka:HL7v2_ADT?brokers=localhost:9092\",\"status\":\"success\"}}";
        String actualBody = mockedExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedBody, actualBody);
    }
}
