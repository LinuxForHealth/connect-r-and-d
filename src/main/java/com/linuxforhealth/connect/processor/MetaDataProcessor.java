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

    /**
     * Supports recursive parsing of Camel simple/{@link SimpleBuilder} expressions.
     * Recursive parsing is useful when a property is used to specify a simple expression.
     *
     * lfh.connect.myprop=\${header.foo}
     *
     * @param simpleExpression The simple expression to parse.
     * @param exchange The current message {@link Exchange}
     * @return the parsed expression as a string.
     */
    private String parseSimpleExpression(String simpleExpression, Exchange exchange) {
        String parsedValue = SimpleBuilder
                .simple(simpleExpression)
                .evaluate(exchange, String.class);

        if (parsedValue != null && parsedValue.startsWith("${") && parsedValue.endsWith("}")) {
            return parseSimpleExpression(parsedValue, exchange);
        }
        return parsedValue;
    }

    /**
     * Replaces a uri placeholder such as <code>{foo}</code> with a simple expression header placeholder such as
     * <code>${header.foo}</code> within an URI.
     *
     * @param uri The URI to process
     * @return the updated URI string
     */
    private String replacePlacholderWithSimpleExpression(String uri) {
        int startingIndex = uri.indexOf("{");
        int endingIndex = uri.indexOf("}");

        String fieldName = uri.substring(startingIndex + 1, endingIndex);
        return uri.replaceAll("\\{" + fieldName + "\\}", "\\$\\{header." + fieldName + "\\}");
    }

    /**
     * Sets metadata fields on the exchange
     * @param exchange The current {@link Exchange}
     * @throws Exception If an error occurs parsing simple expressions
     */
    @Override
    public void process(Exchange exchange) throws Exception {

        exchange.setProperty("routeId", exchange.getFromRouteId());
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("timestamp", Instant.now().getEpochSecond());

        String routeUri = URLDecoder.decode(exchange.getFromEndpoint().getEndpointUri(), StandardCharsets.UTF_8.name());
        if (routeUri.contains("{") && routeUri.contains("}")) {
            routeUri = replacePlacholderWithSimpleExpression(routeUri);
            routeUri = parseSimpleExpression(routeUri, exchange);
        }

        exchange.setProperty("routeUri", routeUri);

        String dataFormatExpression = "${properties:" + routePropertyNamespace + ".dataformat}";
        exchange.setProperty("dataFormat", parseSimpleExpression(dataFormatExpression, exchange).toUpperCase());

        String messageTypeExpression = "${properties:" + routePropertyNamespace + ".messagetype}";
        exchange.setProperty("messageType", parseSimpleExpression(messageTypeExpression, exchange).toUpperCase());

        String topicName = exchange.getProperty("dataFormat") + "_" + exchange.getProperty("messageType");

        exchange.setProperty("dataStoreUri",
                parseSimpleExpression("${properties:lfh.connect.datastore.uri}", exchange)
                .replaceAll("<topicName>", topicName));

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
