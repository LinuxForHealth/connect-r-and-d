/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import java.net.URI;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines Orthanc DICOM Processing routes
 */
public class OrthancRouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(OrthancRouteBuilder.class);

    public final static String POST_ROUTE_ID = "orthanc-post";
    public final static String GET_ROUTE_ID = "orthanc-get";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.orthanc";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport contextSupport = new CamelContextSupport(getContext());
        String orthancServerUri = contextSupport.getProperty("lfh.connect.orthanc_server.uri");
        URI orthancRestUri = URI.create(contextSupport.getProperty("lfh.connect.orthanc.rest.uri"));

        from("{{lfh.connect.orthanc.uri}}")
            .routeId(POST_ROUTE_ID)
            .marshal().mimeMultipart()
            .removeHeaders("Camel*")
            .removeHeaders("Host")
            .to(orthancServerUri)
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                JSONObject obj = new JSONObject(body);
                String id = obj.getString("ID");
                String imageUri = orthancServerUri+"/"+id+"/preview";
                exchange.getIn().setBody(imageUri);
                logger.info("image Uri: "+imageUri);
            })
            .process(new MetaDataProcessor(routePropertyNamespace))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
