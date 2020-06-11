package com.redhat.idaas.connect.processor;

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
 * Tests {@link KafkaToNATS} processor
 */
public class KafkaToNatsTest extends CamelTestSupport {

    private Exchange mockedExchange;
    private KafkaToNATS kafkaToNats;

    private Exchange createMockExchange() {
        TopicPartition mockedTopicPartition = new TopicPartition("HL7v2_ADT", 0);
        RecordMetadata mockedRecordMetadata =  new RecordMetadata(mockedTopicPartition, 0, 0, 1591732928186L, 0L, 0, 0);

        Exchange mockedExchange = new DefaultExchange(context);
        List<RecordMetadata> metaRecords = new ArrayList<>();
        metaRecords.add(mockedRecordMetadata);
        mockedExchange.getIn().setHeader(KafkaConstants.KAFKA_RECORDMETA, metaRecords);

        return mockedExchange;
    }

    /**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() {
        mockedExchange = createMockExchange();
        kafkaToNats = new KafkaToNATS();
    }

    /**
     * Tests {@link KafkaToNATS#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() {
        kafkaToNats.process(mockedExchange);
        String expectedBody = "{\"metadata\":[\"HL7v2_ADT-0@0\"],"+
            "\"results\":[{" +
            "\"partition\":0,\"offset\":0,\"topic\":\"HL7v2_ADT\",\"timestamp\":1591732928186}]}";
        String actualBody = mockedExchange.getIn().getBody(String.class);

        Assertions.assertEquals(expectedBody, actualBody);
    }
}
