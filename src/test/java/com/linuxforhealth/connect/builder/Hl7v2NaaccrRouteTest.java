/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.linuxforhealth.connect.support.TestUtils;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Hl7v2NaaccrRouteTest}
 */
public class Hl7v2NaaccrRouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new Hl7v2RouteBuilder();
    }

    /**
     * Overridden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        HL7MLLPNettyEncoderFactory hl7encoder = new HL7MLLPNettyEncoderFactory();
        HL7MLLPNettyDecoderFactory hl7decoder = new HL7MLLPNettyDecoderFactory();

        context.getRegistry().bind("hl7encoder", hl7encoder);
        context.getRegistry().bind("hl7decoder", hl7decoder);

        mockProducerEndpointById(
                Hl7v2RouteBuilder.ROUTE_ID,
                Hl7v2RouteBuilder.ROUTE_PRODUCER_ID,
                "mock:result"
        );

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testReportNarrativeShort() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("hl7", "ORU_R01_NAACCRv5_Narrative_sections.txt"))
                .replace(System.lineSeparator(), "\r");

        String expectedMessage = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));
        mockResult.expectedMessageCount(1);
        mockResult.expectedPropertyReceived("dataFormat", "HL7-V2");
        mockResult.expectedPropertyReceived("messageType", "NAACCR_CP");
        mockResult.expectedPropertyReceived("naaccrVersion", "VOL_V_50_ORU_R01");

        fluentTemplate.to("netty:tcp://localhost:2576?sync=true&encoders=#hl7encoder&decoders=#hl7decoder")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();

        Exchange mockExchange = mockResult.getExchanges().get(0);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}