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
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link FormatNotificationProcessor} processor
 */
public class FormatNotificationTest extends CamelTestSupport {

    private Exchange mockedExchange;
    private FormatNotificationProcessor formatNotification;

    private Exchange createMockExchange() {
        TopicPartition mockedTopicPartition = new TopicPartition("HL7v2_ADT", 0);
        RecordMetadata mockedRecordMetadata =  new RecordMetadata(mockedTopicPartition, 0, 0, 1591732928186L, 0L, 0, 0);

        List<RecordMetadata> metaRecords = new ArrayList<>();
        metaRecords.add(mockedRecordMetadata);

        Exchange mockedExchange = new DefaultExchange(context);
        mockedExchange.getIn().setHeader("timestamp", "1592514822");
        mockedExchange.adapt(ExtendedExchange.class).setFromRouteId("hl7-v2-mllp");
        mockedExchange.getIn().setHeader(KafkaConstants.KAFKA_RECORDMETA, metaRecords);
        mockedExchange.getIn().setHeader("routeUrl", "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder");
        mockedExchange.getIn().setHeader("dataStoreUrl", "kafka:HL7v2_ADT?brokers=localhost:9092");
        mockedExchange.getIn().setHeader("dataFormat", "hl7-v2");
        mockedExchange.getIn().setHeader("uuid", "123e4567-e89b-42d3-a456-556642441234");

        return mockedExchange;
    }

    /**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() {
        mockedExchange = createMockExchange();
        formatNotification = new FormatNotificationProcessor();
    }

    /**
     * Tests {@link FormatNotificationProcessor#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() {
        formatNotification.process(mockedExchange);
        String expectedBody = "{\"meta\":{\"routeId\":\"hl7-v2-mllp\","+
            "\"uuid\":\"123e4567-e89b-42d3-a456-556642441234\","+
            "\"routeUrl\":\"netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder\","+
            "\"dataFormat\":\"hl7-v2\",\"timestamp\":\"1592514822\","+
            "\"dataStoreUrl\":\"kafka:HL7v2_ADT?brokers=localhost:9092\","+
            "\"status\":\"success\",\"dataRecordLocation\":[\"HL7v2_ADT-0@0\"]}}";
        String actualBody = mockedExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedBody, actualBody);
    }
}
