/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.time.Instant;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

public class AcdAnalyzeProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

        String kafkaDataStoreUri = SimpleBuilder
                .simple("{{lfh.connect.datastore.uri}}")
                .evaluate(exchange, String.class)
                .replaceAll("<topicName>", "ACD_INSIGHTS");

        String routeUrl = SimpleBuilder
                .simple("{{lfh.connect.acd_rest.baseUri}}")
                .evaluate(exchange, String.class);
        
        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", routeUrl);
        exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "acd-container");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("resourceType", "acd-output");
        
        // Set exchange body here
		
	}

}
