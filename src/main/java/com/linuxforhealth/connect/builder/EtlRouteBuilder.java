/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.util.Arrays;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.language.simple.Simple;
import org.apache.camel.model.dataformat.BindyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Defines the ETL processing route.
 */
public class EtlRouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "etl";
    public final static String TRANSFORM_ROUTE_ID = "etl-transform";
    public final static String SAVE_SOURCE_DATA_ROUTE_ID = "etl-save-source";

    private final static String BEAN_PROPERTY_NAMESPACE = "lfh.connect.bean";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.etl";

    private final static String ETL_MESSAGE_TYPE_HEADER = "ETLMessageType";

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

    private void setBeanProperties(Exchange exchange) {
        String messageTypeHeader = exchange.getIn().getHeader(ETL_MESSAGE_TYPE_HEADER, String.class);

        if (messageTypeHeader == null) {
            String message = "Required Header ETLMessageType is not set";
            throw new RuntimeException(message);
        }

        String baseBeanName = messageTypeHeader.replaceAll("[_-]", "");
        
        String formatBeanPropertyName = BEAN_PROPERTY_NAMESPACE + "." + baseBeanName + "format";
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String formatBeanClass = ctxSupport.getProperty(formatBeanPropertyName);
        exchange.setProperty(BINDY_BEAN_PROPERTY, formatBeanClass);

        String transformBean = baseBeanName + "transform";
        exchange.setProperty(TRANSFORM_BEAN_PROPERTY, transformBean);
    }

    @Override
    protected void buildRoute(String routePropertyNamespace)  {

        from("{{lfh.connect.etl.uri}}")
        .routeId(ROUTE_ID)
        .convertBodyTo(String.class)
        // set data format properties based on content type
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
        // cache the original message for later use
        .setProperty(ORIGINAL_MESSAGE_PROPERTY, simple("${body}"))
        .toD("dataformat:${exchangeProperty.bindyDataFormat}:unmarshal?type=${exchangeProperty.bindyType}&classType=${exchangeProperty.bindyBean}")
        .split(body())
            .toD("bean:${exchangeProperty.transformBean}?method=map")
            .toD("${properties:lfh.connect.etl.producerUri}")
            .end()
        .setBody(simple("${exchangeProperty.originalMessage}"))
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
