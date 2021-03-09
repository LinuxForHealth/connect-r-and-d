/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

/**
 * Supports Apple CareKit JSON Transactions
 */
public class CareKitRouteBuilder extends BaseRouteBuilder {

  public final static String ROUTE_ID = "carekit";
  private final static String ORIGINAL_MESSAGE_PROPERTY = "originalMessage";

  @Override
  protected String getRoutePropertyNamespace() {
    return "lfh.connect.carekit";
  }


  @Override
  protected void buildRoute(String routePropertyNamespace) {
    CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
    String carekitUri = ctxSupport.getProperty("lfh.connect.carekit.uri");
    rest(carekitUri)
        .post("/{resource}")
        .route()
        .routeId(ROUTE_ID)
        .setProperty(ORIGINAL_MESSAGE_PROPERTY, simple("${body}"))
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
        .removeHeaders("*")
        .setHeader("Accept", constant("application/json"))
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant(ContentType.APPLICATION_JSON))
        .process(exchange -> {
          JSONObject json = new JSONObject();
          json.put("x12", exchange.getProperty(ORIGINAL_MESSAGE_PROPERTY, String.class));
          exchange.getIn().setBody(json.toString());
        })
        .to("{{lfh.connect.carekit.external.uri}}");
  }
}
