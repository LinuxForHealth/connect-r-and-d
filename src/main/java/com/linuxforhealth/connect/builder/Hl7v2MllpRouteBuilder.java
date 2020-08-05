/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.Hl7v2MetadataProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends RouteBuilder {

    public final static String HL7_V2_MLLP_ROUTE_ID = "hl7-v2-mllp";

    @Override
    public void configure() {
        from("{{lfh.connect.hl7_v2_mllp.uri}}")
                .routeId(HL7_V2_MLLP_ROUTE_ID)
                .unmarshal().hl7()
                .process(new Hl7v2MetadataProcessor())
                .to(LinuxForHealthDirectRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
