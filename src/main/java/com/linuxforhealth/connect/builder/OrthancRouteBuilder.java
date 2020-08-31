/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import com.linuxforhealth.connect.support.LFHKafkaConsumer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines Orthanc DICOM Processing routes
 */
public class OrthancRouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(OrthancRouteBuilder.class);

    public final static String ROUTE_ID = "orthanc-post";
    public final static String ORTHANC_PRODUCER_POST_ID = "orthanc-producer-post";
    public final static String ORTHANC_PRODUCER_GET_ID = "orthanc-producer-get";
    public final static String ORTHANC_PRODUCER_GET_IMAGE_ID = "orthanc-producer-get-image";
    public final static String ORTHANC_PRODUCER_STORE_ID = "orthanc-producer-store";
    public final static String GET_IMAGE_ROUTE_ID = "orthanc-get-png-kafka";
    public final static String GET_IMAGE_CONSUMER_ID = "orthanc-consumer-png-kafka";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.orthanc";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        String orthancServerUri = contextSupport.getProperty("lfh.connect.orthanc_server.uri");
        String orthancExternalUri = contextSupport.getProperty("lfh.connect.orthanc_server.external.uri");

        // Store a DICOM image in Orthanc, convert it to .png and get patient info for the image
        from("{{lfh.connect.orthanc.uri}}")
        .routeId(ROUTE_ID)
        .marshal().mimeMultipart()
        .removeHeaders("Camel*")
        .removeHeaders("Host")
        .to(orthancServerUri)
        .id(ORTHANC_PRODUCER_POST_ID)
        .process(exchange -> {
            // Get the resulting image id & create uri for downstream image retrieval
            String body = exchange.getIn().getBody(String.class);
            JSONObject obj = new JSONObject(body);
            String id = obj.getString("ID");
            exchange.setProperty("imageId", id);
            exchange.setProperty("dataUri", orthancExternalUri+"/"+id+"/preview");

            // Set up for next call to get the .png image
            exchange.setProperty("location", orthancServerUri+"/"+id+"/preview");
            exchange.getIn().removeHeaders("Camel*");
            exchange.getIn().removeHeaders("Content*");
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        })
        .toD("${exchangeProperty[location]}")
        .id(ORTHANC_PRODUCER_GET_IMAGE_ID)
        .process(exchange -> {
            // Get the resulting image bytes
            byte[] body = exchange.getIn().getBody(byte[].class);
            exchange.setProperty("imageBody", Base64.getEncoder().encodeToString(body));

            // Set up for next call to get image tags
            String id = exchange.getProperty("imageId", String.class);
            exchange.setProperty("location", orthancServerUri+"/"+id+"/simplified-tags");
            exchange.getIn().removeHeaders("Camel*");
            exchange.getIn().removeHeaders("Content*");
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        })
        .toD("${exchangeProperty[location]}")
        .id(ORTHANC_PRODUCER_GET_ID)
        .process(exchange -> {
            // set up result object for data storage
            JSONObject tags = new JSONObject(exchange.getIn().getBody(String.class));
            JSONObject result = new JSONObject();
            result.put("patientId", tags.getString("PatientID"));
            result.put("patientName", tags.getString("PatientName"));
            result.put("sourceType", "orthanc");
            result.put("sourceUri", simple("${exchangeProperty[dataUri]}").evaluate(exchange, String.class));
            result.put("image", simple("${exchangeProperty[imageBody]}").evaluate(exchange, String.class));
            exchange.getIn().setBody(result.toString());
            exchange.setProperty("jsonBody", result.toString());
        })
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        .id(ORTHANC_PRODUCER_STORE_ID)
        .process(exchange -> {
            // remove image from response for brevity
            JSONObject result = new JSONObject(exchange.getProperty("jsonBody", String.class));
            result.remove("image");
            LinuxForHealthMessage msg = new LinuxForHealthMessage(exchange);
            msg.setData(Base64.getEncoder().encodeToString(result.toString().getBytes(StandardCharsets.UTF_8)));
            msg.setDataStoreResult(exchange.getIn().getHeader(
                    KafkaConstants.KAFKA_RECORDMETA,
                    new ArrayList<RecordMetadata>(),
                    ArrayList.class));
            exchange.getIn().setBody(msg.toString());
        });

        // Get an .png image from a kafka topic, partition and offset
        from("{{lfh.connect.orthanc.image.uri}}")
        .routeId(GET_IMAGE_ROUTE_ID)
        .bean(LFHKafkaConsumer.class, "get(${header.topic}, ${header.partition}, ${header.offset})")
        .process(exchange -> {
            JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
            String data = new String(Base64.getDecoder().decode(msg.getString("data")));
            String image = new JSONObject(data).getString("image");
            if (image != null) exchange.getIn().setBody(Base64.getDecoder().decode(image));
        })
        .id(GET_IMAGE_CONSUMER_ID);
    }
}
