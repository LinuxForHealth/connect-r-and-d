/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.time.Instant;
import java.util.UUID;

public class AcdAnalyzeProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
	    CamelContextSupport contextSupport = new CamelContextSupport(exchange.getContext());

        String kafkaDataStoreUri = contextSupport
                .getProperty("lfh.connect.dataStore.uri")
                .replaceAll("<topicName>", "ACD_INSIGHTS");

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUri", exchange.getFromEndpoint().getEndpointUri());
        exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "ACD");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("messageType", "INSIGHTS");
	}
}
