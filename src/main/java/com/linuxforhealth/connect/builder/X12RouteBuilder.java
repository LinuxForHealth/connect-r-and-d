/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import com.linuxforhealth.connect.support.LFHMultiResultStrategy;
import com.linuxforhealth.connect.support.X12ParserUtil;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.builder.Builder.bean;

/**
 * Supports X12 Transaction Processing via a REST endpoint.
 * The endpoint accepts a POST request with a raw X12 payload.
 * <code>
 *     {"x12": "ISA*00*          *00*          *ZZ*890069730      *ZZ*154663145..."}
 * </code>
 * The endpoint splits the X12 payload by transaction set (ST segment) and returns an aggregate result.
 */
public class X12RouteBuilder extends BaseRouteBuilder {

    private Logger logger = LoggerFactory.getLogger(X12RouteBuilder.class);

    // route constants
    public static final String X12_REST_ROUTE_ID = "x12";
    public static final String X12_TRANSACTION_ROUTE_ID = "x12-transaction";
    public static final String PROCESS_X12_TRANSACTION_URI = "direct:x12-transaction";
    public static final String METADATA_PROCESSOR_ID = "metadata-processor";
    private final static String ORIGINAL_MESSAGE_PROPERTY = "originalMessage";

    // x12 constants
    public static final int ISA_SEGMENT_LENGTH = 106;

    protected String getSystemLineSeparator() {
        return System.lineSeparator();
    }

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.x12";
    }

    /**
     * Accepts incoming X12 POST requests. Transactions are split and processed in parallel.
     * Transactions are sent via HTTP/REST to the location specified by lfh.connect.x12.external.uri
     * @param routePropertyNamespace The property namespace for the route.
     */
    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String x12Uri = ctxSupport.getProperty("lfh.connect.x12.uri");

        // x12 REST endpoint
        rest(x12Uri)
        .post()
        .route()
        .routeId(X12_REST_ROUTE_ID)
        .unmarshal().json(JsonLibrary.Jackson)
        .setBody(jsonpath("x12"))
        .validate(e -> e.getIn().getBody(String.class).length() > ISA_SEGMENT_LENGTH)
        .log(LoggingLevel.DEBUG, logger, "Parsing X12 Delimiters . . . ")
        .setProperty("fieldDelimiter", simple("${body.substring(3,4)}"))
        .log(LoggingLevel.DEBUG, logger, "Field Delimiter ${exchangeProperty.fieldDelimiter}")
        .setProperty("repetitionCharacter", simple("${body.substring(82,83)}"))
        .log(LoggingLevel.DEBUG, logger, "Repetition Character ${exchangeProperty.repetitionCharacter}")
        .setProperty("componentSeparator", simple("${body.substring(104,105)}"))
        .log(LoggingLevel.DEBUG, logger, "Component Separator ${exchangeProperty.componentSeparator}")
        .setProperty("lineSeparator", simple("${body.substring(105,106)}"))
        .log(LoggingLevel.DEBUG, logger, "Line Separator to ${exchangeProperty.lineSeparator}")
        .log(LoggingLevel.DEBUG, logger, "Completed parsing X12 Delimiters")
        .split(bean(X12ParserUtil.class,
                "split(${body}, ${exchangeProperty.fieldDelimiter}, ${exchangeProperty.lineSeparator})"),
                new LFHMultiResultStrategy())
            .stopOnException()
            .streaming()
            .to(PROCESS_X12_TRANSACTION_URI)
        .end();

        from(PROCESS_X12_TRANSACTION_URI)
        .routeId(X12_TRANSACTION_ROUTE_ID)
        .setHeader("X12MessageType", bean(X12ParserUtil.class,
                "getX12MessageType(${body}, ${exchangeProperty.fieldDelimiter}, ${exchangeProperty.lineSeparator})"))
        .log(LoggingLevel.DEBUG, logger, "Processing X12 Transaction ${header.X12MessageType}")
        .setProperty(ORIGINAL_MESSAGE_PROPERTY, simple("${body}"))
        .process(new MetaDataProcessor(routePropertyNamespace))
        .id(METADATA_PROCESSOR_ID)
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        .removeHeaders("*")
        .setHeader("Accept", constant("application/json"))
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant(ContentType.APPLICATION_JSON))
        .process(exchange -> {
            JSONObject json = new JSONObject();
            json.put("x12",
                exchange.getProperty(ORIGINAL_MESSAGE_PROPERTY, String.class));

            exchange.getIn().setBody(json.toString());
        })
        .to("{{lfh.connect.x12.external.uri}}");
    }
}
