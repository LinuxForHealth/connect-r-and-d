/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.Base64;
import java.net.URLDecoder;

/**
 * Sets Linux for Health Metadata fields using Camel {@link Exchange} properties.
 * Fields set include:
 * <ul>
 *     <li>dataFormat</li>
 *     <li>messageType</li>
 *     <li>routeId</li>
 *     <li>uuid</li>
 *     <li>routeUri</li>
 *     <li>timestamp</li>
 * </ul>
 */
public final class MetaDataProcessor implements Processor {


    private final String routePropertyNamespace;

    @Override
    public void process(Exchange exchange) throws Exception {

        exchange.setProperty("routeId", exchange.getFromRouteId());
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("routeUri", URLDecoder.decode(exchange.getFromEndpoint().getEndpointUri(), StandardCharsets.UTF_8.name()));
        exchange.setProperty("timestamp", Instant.now().getEpochSecond());

        exchange.setProperty("dataFormat",
                SimpleBuilder.simple("${properties:" + routePropertyNamespace + ".dataFormat}")
                        .evaluate(exchange, String.class).toUpperCase());

        exchange.setProperty("messageType",
                SimpleBuilder.simple("${properties:" + routePropertyNamespace + ".messageType}")
                        .evaluate(exchange, String.class).toUpperCase());

        exchange.setProperty("dataStoreUri",
                SimpleBuilder.simple("${properties:lfh.connect.dataStore.uri}")
                        .evaluate(exchange, String.class)
                        .replaceAll("<topicName>",
                                exchange.getProperty("dataFormat") + "_" + exchange.getProperty("messageType")));

        String exchangeBody = exchange.getIn().getBody(String.class);
        String result = Base64.getEncoder().encodeToString(exchangeBody.getBytes(StandardCharsets.UTF_8));
        exchange.getIn().setBody(result);
    }

    /**
     * Creates a new instance, associating it with the specified route property namespace.
      * @param routePropertyNamespace The property namespace of the route creating the instance.
     */
    public MetaDataProcessor(String routePropertyNamespace) {
        this.routePropertyNamespace = routePropertyNamespace;
    }
}
