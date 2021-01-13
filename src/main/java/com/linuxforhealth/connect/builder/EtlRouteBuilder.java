/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.BindyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Defines the ETL processing route.
 * 
 * The ETL processing route utilizes data format and transformation beans to
 * support custom inbound and destination formats. Bindy is used
 * to provide data parsing services.
 * 
 * Processing is driven by the ETLMessageType and Content-Type headers.
 * The Content-Type header, set to either text/csv or text/plain, is set
 * based on the inboud message format. text/csv is used for CSV, while
 * text/plain is used for FIXED length.
 * 
 * The ETLMessageType identifies the data format and transformation bean
 * by using a standard naming scheme: [domain]_[csv|fixed].
 * For example if ETLMessageType is set to practitioner_csv, the route will
 * resolve the data format and transform beans using the properties 
 * com.linuxforhealth.etl.practitionercsvformat (data format)
 * AND
 * com.linuxforhealth.etl.practitionercsvtransform (transform)
 * 
 * The ETL route's beans and destination uri are configured via application.properties.
 * <ul>
 *  <li>lfh.connect.bean.[record type][csv|fixed]format: The ETL format bean</li>
 *  <li>lfh.connect.bean.[record type][csv|fixed]transform: The ETL transform bean</li>
 *  <li>lfh.connect.etl.producerUri: The target system for the ETL payload</li>
 * </ul>
 */
public class EtlRouteBuilder extends BaseRouteBuilder {

    // route constants
    public final static String ROUTE_ID = "etl";
    public final static String PRODUCER_ID = "etl-producer";
    public final static String DATA_PERSIST_PRODUCER_ID = "etl-save-source";
    private final static String BEAN_PROPERTY_NAMESPACE = "lfh.connect.bean";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.etl";
    private final static String ETL_MESSAGE_TYPE_HEADER = "ETLMessageType";

    // data transformation constants
    private final static String BINDY_DATA_FORMAT_PROPERTY = "bindyDataFormat";
    private final static String BINDY_TYPE_PROPERTY = "bindyType";
    private final static String BINDY_BEAN_PROPERTY = "bindyBean";
    private final static String ORIGINAL_MESSAGE_PROPERTY = "originalMessage";
    private final static String TRANSFORM_BEAN_PROPERTY = "transformBean";

    private final Logger logger = LoggerFactory.getLogger(EtlRouteBuilder.class);    

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    /**
     * Sets Exchange Properties for the ETL format and transform beans based on
     * the incoming ETLMessageType header.
     * Properties include:
     * <ul>
     *  <li>BINDY_BEAN_PROPERTY: The fully qualified class name for the
     *      parsing/dataformat bean.
     * </li>
     *  <li>TRANSFORM_BEAN_PROPERTY: The "lookup" name for the ETL transform
     *      bean in the Camel registry.
     *  </li>
     * </ul>
     * @param exchange
     */
    private void setBeanProperties(Exchange exchange) {
        String messageTypeHeader = exchange.getIn().getHeader(ETL_MESSAGE_TYPE_HEADER, String.class);

        if (messageTypeHeader == null) {
            String message = "Required Header ETLMessageType is not set";
            throw new RuntimeException(message);
        }

        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String baseBeanName = messageTypeHeader.replaceAll("[_]", "");
        
        String formatBeanClass = ctxSupport.getProperty(
                                 BEAN_PROPERTY_NAMESPACE + "." +
                                 baseBeanName + "format");
        exchange.setProperty(BINDY_BEAN_PROPERTY, formatBeanClass);

        String transformBean = baseBeanName + "transform";
        exchange.setProperty(TRANSFORM_BEAN_PROPERTY, transformBean);
    }

    /**
     * Defines the ETLRoute
     */
    @Override
    protected void buildRoute(String routePropertyNamespace)  {

        from("{{lfh.connect.etl.uri}}")
        .routeId(ROUTE_ID)
        .convertBodyTo(String.class)
        // validate ETLMessageType header value
        .choice()
            .when(simple("${header.ETLMessageType} !regex '[a-z]+_(csv|fixed)'"))
                .throwException(RuntimeException.class, "Invalid ETLMessageType Format ${header.ETLMessageType}")
        .end()
        // set data format properties based on content type
        .log(LoggingLevel.DEBUG, logger, "Processing Content-Type ${header.Content-Type}")
        .choice()
            .when(simple("${header.Content-Type} =~ 'text/csv'"))
                .setProperty(BINDY_DATA_FORMAT_PROPERTY, constant("bindy-csv"))
                .setProperty(BINDY_TYPE_PROPERTY, constant(BindyType.Csv.name()))
            .when(simple("${header.Content-Type} =~ 'text/plain'"))
                .setProperty(BINDY_DATA_FORMAT_PROPERTY, constant("bindy-fixed"))
                .setProperty(BINDY_TYPE_PROPERTY, constant(BindyType.Fixed.name()))
            .otherwise()
                .throwException(RuntimeException.class, "Unsupported content-type ${header.Content-Type}")
        .end()
        // configure format and transform beans
        .process(exchange -> {
            setBeanProperties(exchange);
        })
        .log(LoggingLevel.DEBUG, logger, "Bindy Data Format Component: ${exchangeProperty.bindyDataFormat}")
        .log(LoggingLevel.DEBUG, logger, "Bindy Type: ${exchangeProperty.bindyType}")
        .log(LoggingLevel.DEBUG, logger, "Bindy Bean: ${exchangeProperty.bindyBean}")
        .log(LoggingLevel.DEBUG, logger, "Transform Bean: ${exchangeProperty.transformBean}")
        // cache the original message for LFH persistence
        .setProperty(ORIGINAL_MESSAGE_PROPERTY, simple("${body}"))
        .toD("dataformat:${exchangeProperty.bindyDataFormat}:unmarshal?type=${exchangeProperty.bindyType}&classType=${exchangeProperty.bindyBean}")
        .split(body())
            .toD("bean:${exchangeProperty.transformBean}?method=map")
            .toD("${properties:lfh.connect.etl.producerUri}")
        .end()
        .setBody(simple("${exchangeProperty.originalMessage}"))
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        .id(DATA_PERSIST_PRODUCER_ID);
    }
}
