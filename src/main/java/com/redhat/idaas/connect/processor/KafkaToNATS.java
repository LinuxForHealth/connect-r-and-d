package com.redhat.idaas.connect.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Creates the NATS message for a message stored in Kafka
 */
public class KafkaToNATS implements Processor {

    public void process(Exchange exchange) throws Exception {
        ArrayList<RecordMetadata> meta = exchange.getIn().getHeader(KafkaConstants.KAFKA_RECORDMETA, ArrayList.class);
        JSONArray results = new JSONArray();
        JSONObject topObj = new JSONObject();
        JSONArray metaJson  = new JSONArray();
        ListIterator itr = meta.listIterator();

        while(itr.hasNext()) {
            RecordMetadata m = (RecordMetadata) itr.next();
            JSONObject jsonObj = new JSONObject();
            if (m.hasTimestamp()) {
                jsonObj.put("timestamp", m.timestamp());
            }
            jsonObj.put("topic", m.topic());
            jsonObj.put("partition", m.partition());
            jsonObj.put("offset", m.offset());
            results.put(jsonObj);
            metaJson.put(m);
        }

        topObj.put("results", results);
        topObj.put("metadata", metaJson);

        exchange.getIn().setBody(topObj.toString());
    }
}