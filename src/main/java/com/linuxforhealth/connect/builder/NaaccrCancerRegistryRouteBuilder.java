/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a NAACCR Cancer Registry processing route
 */
public class NaaccrCancerRegistryRouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "Naaccr-get-registry-xml";
    public final static String ROUTE_PRODUCER_ID="Naaccr-xml";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.naaccr.registry";
    
    private final Logger logger = LoggerFactory.getLogger(NaaccrCancerRegistryRouteBuilder.class);

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

         // Get patients from NAACCR-XML kafka topic, partition and offset
         from("{{"+ROUTE_PROPERTY_NAMESPACE+".uri}}")
         .routeId(ROUTE_ID)
         .setHeader("topic", constant("NAACCR"))
         .setHeader("partition", constant(0))
         .bean("bean:LFHKafkaConsumer", "getAll(${header.topic}, ${header.partition})")
         .process(exchange -> {
            List<String> list = exchange.getIn().getBody(List.class);

            //aggregate patient cohort for cancer registry using NAACCR XML standard
            String result = aggregate(list);

            exchange.getMessage().setBody(result);
         });
       
    }

    private String aggregate(List<String> reports) {

        StringBuilder buf = new StringBuilder();

        buf.append(NAACCR_XML_ENCODING_TAG+"\n");

        try {
            ZonedDateTime date = ZonedDateTime.now();
            String timeGenerated = date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            buf.append(NAACCR_XML_PARENT_START_TAG.replace(NAACCR_XML_TIMEGEN_PLACEHOLDER, timeGenerated)+"\n");

            for(String report : reports) {
                String data = new JSONObject(report).getString("data");
                buf.append(data+ "\n");
            }
        }catch(Exception e) {
           logger.error(e.getMessage());
        }

        buf.append(NAACCR_XML_PARENT_END_TAG+"\n");

        return buf.toString();

    }

    private static final String NAACCR_XML_TIMEGEN_PLACEHOLDER = "[DATETIME_PLACEHOLDER]";

    private static final String NAACCR_XML_ENCODING_TAG = "<?xml version=\"1.0\"?>";

    private static final String NAACCR_XML_PARENT_START_TAG = "<NAACCRData baseDictionaryUri=\"http://naaccr.org/naaccrxml/naaccr-dictionary-210.xml\" recordType=\"I\" timeGenerated=\""+NAACCR_XML_TIMEGEN_PLACEHOLDER+"\" specificationVersion=\"1.4\" xmlns=\"http://naaccr.org/naaccrxml\">";
   
    private static final String NAACCR_XML_PARENT_END_TAG = "</NAACCRData>";

}
