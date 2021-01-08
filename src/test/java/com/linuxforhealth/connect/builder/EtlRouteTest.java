/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.util.Properties;

import com.linuxforhealth.connect.support.TestUtils;
import com.linuxforhealth.connect.support.etl.PractitionerCsvFormat;

import org.apache.camel.RoutesBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link EtlRouteBuilder}
 */
public class EtlRouteTest extends RouteTestSupport{

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new EtlRouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.bean.etl", "com.linuxforhealth.connect.support.etl.PractitionerCsvFormat");
        return props;
    }

    @Override
    protected void configureContext() throws Exception {
        context.getRegistry().bind("etl", PractitionerCsvFormat.class, new PractitionerCsvFormat());
        super.configureContext();
    }

    @Test
    void testRoute() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("etl", "practitioner.csv"));
        Assertions.assertNotNull(testMessage);
    }

}
