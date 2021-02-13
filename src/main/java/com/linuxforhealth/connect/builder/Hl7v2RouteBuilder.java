/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.linuxforhealth.connect.processor.Hl7NaaccrProcessor;
import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.FHIRConverter;


/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2RouteBuilder extends BaseRouteBuilder {

  public static final String ROUTE_ID = "hl7-v2";

  public static final String ROUTE_PRODUCER_ID = "hl7-v2";
  public static final String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.hl7-v2";
  public static final String HTTP_ROUTE_ID = "hl7-v2-http";
  public static final String NAACCR_ROUTE_ID = "hl7-v2-naaccr";
  public static final String NAACCR_PRODUCER_ID = "hl7-v2-naaccr-producer";
  public static final String HL7_FHIR_ROUTE_ID = "hl7-v2-fhir";
  public static final String HL7_FHIR_PRODUCER_ID = "hl7-v2-fhir-producer";
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7NaaccrProcessor.class);

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        //Route for MLLP origin
        from("{{lfh.connect.hl7-v2.uri}}")
                .routeId(ROUTE_ID)
                .unmarshal().hl7()
                .process(new MetaDataProcessor(routePropertyNamespace))
                .multicast()
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "direct:" + NAACCR_ROUTE_ID,
            "direct:" + HL7_FHIR_ROUTE_ID)
                .id(ROUTE_PRODUCER_ID);

        //Route for HTTP origin
        from("{{lfh.connect.hl7-v2.http.uri}}")
                .routeId(HTTP_ROUTE_ID)
                .unmarshal().hl7()
                .process(new MetaDataProcessor(routePropertyNamespace))
                .multicast()
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "direct:" + NAACCR_ROUTE_ID,
            "direct:" + HL7_FHIR_ROUTE_ID)
                .id(HTTP_ROUTE_ID);

        //Route for NAACCR HL7 Subprotocol for Electronic Pathology Reports
        from("direct:"+NAACCR_ROUTE_ID)
                .routeId(NAACCR_ROUTE_ID)
                .process(new Hl7NaaccrProcessor(routePropertyNamespace))
                .choice()
                .when(isHeaderSet)
                    .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
                    .id(NAACCR_PRODUCER_ID)  
                .otherwise()
        .log(LoggingLevel.INFO, LOGGER, "non-NAACCR report")
                    .stop()
                .end();

    // Route for HL7 to FHIR conversion
    from("direct:" + HL7_FHIR_ROUTE_ID)
        .routeId(HL7_FHIR_ROUTE_ID)
        .choice()
        .when(simple("${properties:lfh.connect.hl7-v2.convertToFhir:false} == 'true'"))
        .bean(FHIRConverter.class, "convert")
        .multicast()
        .to(FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_URI, LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI).id(HL7_FHIR_PRODUCER_ID)
        .endChoice()
        .otherwise()
        .log(LoggingLevel.INFO, LOGGER, "No FHIR conversion required")
        .end();


    }

    //used as predicate to detect whether to process as an NAACCR message
    private final Predicate isHeaderSet = header("naaccrReportType").isNotNull();

}
