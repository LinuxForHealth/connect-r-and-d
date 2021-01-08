/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.util.Arrays;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import org.apache.camel.LoggingLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Defines the ETL processing route.
 * The ETL route is a data processing route which supports custom source and target data formats via custom components.
 * 
 * Example property configuration:
 *  lfh.connect.bean.practitionercsvformat=com.linuxforhealth.connect.support.etl.PractitionerCsvFormat
 *  lfh.connect.bean.practitionercsvtransform=com.linuxforhealth.connect.support.etl.PractitionerCsvTransform
 *  lfh.connect.etl.uri=jetty:http://{{lfh.connect.host}}:{{lfh.connect.http.port}}/etl?httpMethodRestrict=POST&enableMultipartFilter=true
 *  lfh.connect.etl.dataformat=etl
 *  lfh.connect.etl.messagetype=\${header.ETLMessageType}
 *  lfh.connect.etl.target=SomeSystem
 * 
 *  "Bean" properties specify the format and transform components used in the route.
 *  The format bean is used to store the marshalled data payload, while the transform bean converts source data to a target format.
 *  The ETLMessageType header is used to specify which components are loaded for the incoming request, and is set dynamically.
 *  The components used in route are determined using the following algorithm:
 *  - remove any "_" or "-" from the ETLMessageType and then convert to lower-case
 *  - lookup component names [converted ETLMessageType]csv and [convertedETLMessageType]format.
 * 
 *  Source data is stored in a topic named ETL_<ETlMessageType Header>
 *  Source data is processed a record at a time, and then submitted to the destination defined in lfh.connect.etl.target
 *  
 */
public class EtlRouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "etl";
    public final static String TRANSFORM_ROUTE_ID = "etl-transform";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.etl";
    private final static String MESSAGE_TYPE_HEADER = "ETLMessageType";

    private final Logger logger = LoggerFactory.getLogger(EtlRouteBuilder.class);    

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace)  {

        from("{{lfh.connect.etl.uri}}")
        .routeId(ROUTE_ID)
        .choice()
            .when(header(MESSAGE_TYPE_HEADER).isNull())
            .setHeader(MESSAGE_TYPE_HEADER, constant("SOURCE_DATA"))
        .otherwise()
            .process(exchange -> {
                String messageTypeHeader = exchange.getIn().getHeader(MESSAGE_TYPE_HEADER).toString();
                validateEtlComponents(messageTypeHeader);
            })
        .end()
        .marshal().mimeMultipart()
        .process(new MetaDataProcessor(routePropertyNamespace))
        .choice()
            .when(simple("${header:" + MESSAGE_TYPE_HEADER + "} == 'SOURCE_DATA'"))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        .otherwise()
            .multicast()
            .parallelProcessing()
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "direct:" + TRANSFORM_ROUTE_ID)
        .end();

        from("direct:" + TRANSFORM_ROUTE_ID)
        .id(TRANSFORM_ROUTE_ID)
        .process(exchange -> {
            


        })
        .log(LoggingLevel.INFO, logger, "I got here");
    }

    /**
     * Returns the base component name based on the requested message type header.
     * Example: practitioner-csv returns practitionercsv
     * @param messageTypeHeader The ETL Route Message Type Header (ETLMessageType)
     * @return the base component name
     */
    private String getBaseComponentName(String messageTypeHeader) {
        return messageTypeHeader.replace("_", "").replace("-", "");
    }

    /**
     * Validates that a format and transform component have been loaded to support the ETL message type.
     * @param messageTypeHeader The ETL message type header.
     */
    private void validateEtlComponents(String messageTypeHeader) {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());

        String baseComponentName = getBaseComponentName(messageTypeHeader);
        for (String componentName : Arrays.asList(baseComponentName + "transform", baseComponentName + "format")) {
            if (ctxSupport.getComponent(componentName) == null) {
                String message = "Unable to load " + componentName + " for ETL Message Header " + messageTypeHeader;
                throw new RuntimeException(message);
            }
        }
    }
}
