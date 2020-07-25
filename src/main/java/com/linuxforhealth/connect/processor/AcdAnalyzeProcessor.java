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
                .getProperty("lfh.connect.datastore.uri")
                .replaceAll("<topicName>", "ACD_INSIGHTS");

        String routeUrl = contextSupport.getProperty("lfh.connect.acd_rest.baseUri");

        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", routeUrl);
        exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "acd-container");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("resourceType", "acd-output");
        
        // Set exchange body here
		
	}

}
