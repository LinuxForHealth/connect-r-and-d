/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.LinuxForHealthMessage;
import com.linuxforhealth.connect.processor.MetaDataProcessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.apache.camel.Exchange;
import org.apache.camel.builder.SimpleBuilder;
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

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.orthanc";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

       /**
        * Stores a DICOM image in Orthanc, converts it to .png and gets patient info for the image.
        *
        * This route uses 3 separate calls to the Orthanc DICOM server deployed via LinuxForHealth:
        * 1. POST the input DICOM image to Orthanc
        * 2. GET the .png version of the same image
        * 3. GET the patient details (name and ID) from the DICOM image
        *
        * 4 processors are used: The first 2 process the results of an Orthanc call and set up the next call.  
        * The third sets up the data for storage in the data store and the 4th removes the .png image from the 
        * final returned result, due to the size of the image.
        *
        * To access the image for further downstream processing, use a NATS subscriber.  A default 
        * NATS subscriber that listens for notifications from the local LinuxForHealth instance is deployed
        * by default with LinuxForHealth.
        */
        from("{{lfh.connect.orthanc.uri}}")
        .routeId(ROUTE_ID)
        .marshal().mimeMultipart()
        .removeHeaders("Camel*")
        .removeHeaders("Host")
        .to("{{lfh.connect.orthanc.server.uri}}")
        .id(ORTHANC_PRODUCER_POST_ID)
        .process(exchange -> {
            String orthancServerUri = SimpleBuilder
                .simple("${properties:lfh.connect.orthanc.server.uri}")
                .evaluate(exchange, String.class);

            // Get the resulting image id & create uri for downstream image retrieval
            String body = exchange.getIn().getBody(String.class);
            JSONObject obj = new JSONObject(body);
            String id = obj.getString("ID");
            exchange.setProperty("imageId", id);
            exchange.setProperty("dataUri", "/instances/"+id+"/preview");

            // Set up for next call to get the .png image
            exchange.setProperty("location", orthancServerUri+"/"+id+"/preview");
            exchange.getIn().removeHeaders("Camel*");
            exchange.getIn().removeHeaders("Content*");
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        })
        .toD("${exchangeProperty[location]}")
        .id(ORTHANC_PRODUCER_GET_IMAGE_ID)
        .process(exchange -> {
            String orthancServerUri = SimpleBuilder
                .simple("${properties:lfh.connect.orthanc.server.uri}")
                .evaluate(exchange, String.class);

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
        .bean("bean:LFHKafkaConsumer", "get(${header.topic}, ${header.partition}, ${header.offset})")
        .process(exchange -> {
            JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
            String data = new String(Base64.getDecoder().decode(msg.getString("data")));
            String image = new JSONObject(data).getString("image");
            if (image != null) exchange.getIn().setBody(Base64.getDecoder().decode(image));
        });
    }
}
