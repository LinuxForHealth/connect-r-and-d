/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import org.apache.camel.Exchange;
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
    public final static String ORTHANC_PRODUCER_STORE_NOTIFY_ID = "orthanc-producer-store-notify";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.orthanc";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        String orthancServerUri = contextSupport.getProperty("lfh.connect.orthanc_server.uri");

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
                exchange.setProperty("dataUri", orthancServerUri+"/"+id+"/preview");

                // Set up for next call to get image tags
                exchange.setProperty("location", orthancServerUri+"/"+id+"/simplified-tags");
                exchange.getIn().removeHeaders("Camel*");
                exchange.getIn().removeHeaders("Content*");
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
            })
            .toD("${exchangeProperty[location]}")
            .id(ORTHANC_PRODUCER_GET_ID)
            .process(exchange -> {
                JSONObject tags = new JSONObject(exchange.getIn().getBody(String.class));
                JSONObject result = new JSONObject();
                result.put("patientId", tags.getString("PatientID"));
                result.put("patientName", tags.getString("PatientName"));
                result.put("sourceType", "orthanc");
                result.put("sourceUri", simple("${exchangeProperty[dataUri]}").evaluate(exchange, String.class));
                exchange.getIn().setBody(result.toString());
                logger.info("result: "+result.toString());
            })
            .process(new MetaDataProcessor(routePropertyNamespace))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
            .id(ORTHANC_PRODUCER_STORE_NOTIFY_ID);
    }
}
