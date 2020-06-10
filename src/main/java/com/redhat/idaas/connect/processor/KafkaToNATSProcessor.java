package com.redhat.idaas.connect.processor;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Creates the NATS message for a message stored in Kafka
 */
public class KafkaToNATSProcessor implements Processor {

    public void process(Exchange exchange)  {
        List<RecordMetadata> metaRecords = exchange
                        .getIn()
                        .getHeader(KafkaConstants.KAFKA_RECORDMETA, ArrayList.class);

        JSONArray results = new JSONArray();
        JSONObject topObj = new JSONObject();
        JSONArray metaJson  = new JSONArray();

        for (RecordMetadata metaRecord: metaRecords) {
            JSONObject jsonObj = new JSONObject();
            if (metaRecord.hasTimestamp()) {
                jsonObj.put("timestamp", metaRecord.timestamp());
            }
            jsonObj.put("topic", metaRecord.topic());
            jsonObj.put("partition", metaRecord.partition());
            jsonObj.put("offset", metaRecord.offset());
            results.put(jsonObj);
            metaJson.put(metaRecord);
        }

        topObj.put("results", results);
        topObj.put("metadata", metaJson);

        exchange.getIn().setBody(topObj.toString());
    }
}
