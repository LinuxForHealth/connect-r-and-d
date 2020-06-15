/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.processor.LinuxForHealthProcessor;
import java.util.List;
import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Format the message for data storage notification 
 */
public class FormatNotificationProcessor extends LinuxForHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange)  {
        JSONObject top = new JSONObject();
        JSONObject meta = new JSONObject();
        JSONArray kafkaMeta  = new JSONArray();

        setCommonMeta(exchange, meta);
        meta.put("dataStoreUrl", exchange.getIn().getHeader("dataStoreUrl", String.class));
        meta.put("status", "success");

        List<RecordMetadata> metaRecords = exchange.getIn().getHeader(KafkaConstants.KAFKA_RECORDMETA, ArrayList.class);
        for (RecordMetadata m: metaRecords) {
            kafkaMeta.put(m);
        }
        meta.put("dataRecordLocation", kafkaMeta);
        top.put("meta", meta);

        exchange.getIn().setBody(top.toString());
    }
}
