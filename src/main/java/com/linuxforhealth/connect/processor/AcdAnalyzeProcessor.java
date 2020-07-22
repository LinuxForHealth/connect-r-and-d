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

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;

public class AcdAnalyzeProcessor extends LinuxForHealthProcessor implements Processor {

	public AcdAnalyzeProcessor() { }

	@Override
	public void process(Exchange exchange) throws Exception {
		
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("ACD_INSIGHTS");
        String routeUrl = uriBuilder.getAcdRestUri();
        
        exchange.setProperty("timestamp", Instant.now().getEpochSecond());
        exchange.setProperty("routeUrl", routeUrl);
        exchange.setProperty("dataStoreUri", kafkaDataStoreUri);
        exchange.setProperty("dataFormat", "acd-container");
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("resourceType", "acd-output");
        
        // Set exchange body here
		
	}

}
