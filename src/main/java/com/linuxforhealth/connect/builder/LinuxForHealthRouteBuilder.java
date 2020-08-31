/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

/**
 * Defines the Linux for Health "internal" routes for data storage, notification, and error handling
 */
public final class LinuxForHealthRouteBuilder extends RouteBuilder {

    public final static String STORE_AND_NOTIFY_CONSUMER_URI = "direct:storeAndNotify";
    public final static String STORE_CONSUMER_URI = "direct:store";
    public final static String NOTIFY_CONSUMER_URI = "direct:notify";
    public final static String ERROR_CONSUMER_URI = "direct:error";

    public final static String STORE_AND_NOTIFY_ROUTE_ID = "store-and-notify";
    public final static String STORE_ROUTE_ID = "lfh-store";
    public final static String STORE_PRODUCER_ID = "lfh-store-producer";
    public final static String NOTIFY_ROUTE_ID = "lfh-notify";
    public final static String NOTIFY_PRODUCER_ID = "lfh-notify-producer";
    public final static String ERROR_ROUTE_ID = "lfh-error";
    public final static String ERROR_PRODUCER_ID = "lfh-error-producer";
    public final static String REMOTE_EVENTS_ROUTE_ID = "lfh-remote-events";
    public final static String REMOTE_EVENTS_PRODUCER_ID = "lfh-remote-events-producer";

    private final Logger logger = LoggerFactory.getLogger(LinuxForHealthRouteBuilder.class);

    @Override
    public void configure() {
        // Store results in the data store and send a notification message
        from(STORE_AND_NOTIFY_CONSUMER_URI)
        .routeId(STORE_AND_NOTIFY_ROUTE_ID)
        .to(STORE_CONSUMER_URI)
        .to(NOTIFY_CONSUMER_URI);

        // Store results in the data store
        from(STORE_CONSUMER_URI)
        .routeId(STORE_ROUTE_ID)
        .process(exchange -> {
            LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
            msg.setData(exchange.getIn().getBody());
            exchange.getIn().setBody(msg.toString());
        })
        .toD("${exchangeProperty[dataStoreUri]}")
        .id(STORE_PRODUCER_ID);

        // Send a notification message based on the data storage results
        from(NOTIFY_CONSUMER_URI)
        .routeId(NOTIFY_ROUTE_ID)
        .process(exchange -> {
            LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
            msg.setDataStoreResult(exchange.getIn().getHeader(
                    KafkaConstants.KAFKA_RECORDMETA,
                    new ArrayList<RecordMetadata>(),
                    ArrayList.class));
            JSONObject jsonMsg = new JSONObject(exchange.getIn().getBody(String.class));
            if(jsonMsg.has("data")) msg.setData(jsonMsg.getString("data"));
            exchange.getIn().setBody(msg.toString());
        })
        .to("{{lfh.connect.messaging.uri}}")
        .id(NOTIFY_PRODUCER_ID);

        // Send an error notification message
        from(ERROR_CONSUMER_URI)
        .routeId(ERROR_ROUTE_ID)
        .process(exchange -> {
            final Throwable exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
            LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
            msg.setError(exception.getMessage());
            exchange.getIn().setBody(msg.toString());
        })
        .log(LoggingLevel.ERROR, logger, exceptionMessage().toString())
        .to("{{lfh.connect.messaging.uri}}")
        .id(ERROR_PRODUCER_ID);

        // Consume message from kafka lfh-remote-events topic and store in correct kafka topic
        from("{{lfh.connect.datastore.remote-events.consumer.uri}}")
        .routeId(REMOTE_EVENTS_ROUTE_ID)
        .process(exchange -> {
            // Create the LFH message envelope
            JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
            JSONObject meta = msg.getJSONObject("meta");
            exchange.setProperty("routeId", meta.getString("routeId"));
            exchange.setProperty("uuid", meta.getString("uuid"));
            exchange.setProperty("timestamp", Instant.now().getEpochSecond());
            exchange.setProperty("dataFormat", meta.getString("dataFormat"));
            exchange.setProperty("messageType", meta.getString("messageType"));
            exchange.setProperty("routeUri", meta.getString("routeUri"));
            exchange.setProperty("dataStoreUri", meta.getString("dataStoreUri"));
            exchange.getIn().setBody(msg.getString("data"));
        })
        .toD(STORE_CONSUMER_URI)
        .id(REMOTE_EVENTS_PRODUCER_ID);
    }
}
