/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import com.linuxforhealth.connect.support.LFHMultiResultStrategy;
import com.linuxforhealth.connect.support.x12.IsaValidatingParser;
import com.linuxforhealth.connect.support.x12.X12RouteRequest;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 * Supports X12 Transaction Processing via a REST endpoint.
 * The endpoint accepts a POST request with a raw X12 payload.
 * <code>
 *     {"x12": "ISA*00*          *00*          *ZZ*890069730      *ZZ*154663145..."}
 * </code>
 * The endpoint splits the X12 payload by transaction set (ST segment) and returns an aggregate result.
 */
public class X12RouteBuilder extends BaseRouteBuilder {

    public static final String ROUTE_ID = "x12";

    private final String systemLineSeparator = System.lineSeparator();

    protected String getSystemLineSeparator() {
        return systemLineSeparator;
    }

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.x12";
    }

    /**
     * Accepts incoming X12 POST requests. Transactions are split
     *
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
        .routeId(ROUTE_ID)
        .unmarshal().json(JsonLibrary.Jackson, X12RouteRequest.class)
        .process( e-> {
            X12RouteRequest request = e.getIn().getBody(X12RouteRequest.class);
            e.getIn().setBody(request.getX12(), String.class);
        })
        .transform(body().regexReplaceAll(getSystemLineSeparator(), ""))
        .split(method("x12splitter", "split"), new LFHMultiResultStrategy())
        .parallelProcessing()
        .process( e-> {
            String x12Transaction = e.getIn().getBody(String.class);

            String isaSegment = x12Transaction.substring(0, IsaValidatingParser.ISA_SEGMENT_LENGTH);
            IsaValidatingParser isaParser = new IsaValidatingParser(isaSegment);
            String x12MessageType = getX12MessageType(
                    x12Transaction,
                    isaParser.getFieldDelimiter(),
                    isaParser.getLineSeparator());

            e.getIn().setHeader("X12MessageType", x12MessageType);
        })
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }

    /**
     * Parses the X12 Message Type from the ST segment transaction code and specification version
     * Example ST Segment:
     * <code>
     *     ST*270*0010*005010X279A1~
     * </code>
     * The transaction code is in the second field, delimited by a "*"
     *
     * @param x12Transaction The X12 transaction string
     * @param fieldDelimiter The field delimiter character
     * @param lineSeparator The line separator character
     * @return The X12 message type for the transaction
     */
    private String getX12MessageType(String x12Transaction, String fieldDelimiter, String lineSeparator) {

        int transactionStart = x12Transaction.indexOf(lineSeparator + "ST") + 1;
        int transactionEnd = x12Transaction.indexOf(lineSeparator, transactionStart);
        String splitCharacter = fieldDelimiter;

        if (splitCharacter.equals("*") || splitCharacter.equals("|") || splitCharacter.equals("?")) {
            splitCharacter = "\\" + splitCharacter;
        }

        String[] transactionHeaderFields = x12Transaction
                .substring(transactionStart, transactionEnd)
                .split(splitCharacter);

        return transactionHeaderFields[1];
    }
}
