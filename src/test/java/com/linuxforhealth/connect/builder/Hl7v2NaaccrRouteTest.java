/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.linuxforhealth.connect.processor.Hl7NaaccrProcessor;
import com.linuxforhealth.connect.support.TestUtils;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.mock.MockEndpoint;
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
                Hl7v2RouteBuilder.NAACCR_ROUTE_ID,
                Hl7v2RouteBuilder.NAACCR_PRODUCER_ID,
                "mock:result"
        );
  
        
        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testReportSynopticItemized() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("hl7",  "ORU_R01_NAACCRv5_Synoptic_CAPeCC_itemized_short.txt"))
                .replace(System.lineSeparator(), "\r");

        mockResult.expectedMessageCount(1);

        String encodedTestMsg = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.MESSAGE_TYPE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.NAACCR_MESSAGE_TYPE);
        
        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE_SYNOPTIC_ITEMIZED);

        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.HISTOLOGY_TYPE_ICDO3, "C24.1");
         
        fluentTemplate.to("direct:"+Hl7v2RouteBuilder.NAACCR_ROUTE_ID)
                .withBody(encodedTestMsg)
                .send();

        mockResult.assertIsSatisfied();

        String expectedMessage = new String(Base64.getDecoder().decode(mockResult.getExchanges().get(0).getIn().getBody(String.class)));
        assertNotNull(expectedMessage, "response body null");
       
    }

    @Test
    void testReportNarrative() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("hl7",  "ORU_R01_NAACCRv5_Narrative_sections.txt"))
                .replace(System.lineSeparator(), "\r");

        mockResult.expectedMessageCount(1);

        String encodedTestMsg = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.MESSAGE_TYPE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.NAACCR_MESSAGE_TYPE);
        
        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE_NARRATIVE);
        
        //TODO could/should do more validation here of other metadata
         
        fluentTemplate.to("direct:"+Hl7v2RouteBuilder.NAACCR_ROUTE_ID)
                .withBody(encodedTestMsg)
                .send();

        mockResult.assertIsSatisfied();

        String expectedMessage = new String(Base64.getDecoder().decode(mockResult.getExchanges().get(0).getIn().getBody(String.class)));
        assertNotNull(expectedMessage, "response body null");
       
    }


    @Test
    void testReportSynopticSections() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("hl7",  "ORU_R01_NAACCRv5_Synoptic_CAPeCC_sections_long.txt"))
                .replace(System.lineSeparator(), "\r");

        mockResult.expectedMessageCount(1);

        String encodedTestMsg = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.MESSAGE_TYPE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.NAACCR_MESSAGE_TYPE);
        
        mockResult.expectedPropertyReceived(Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE, 
            Hl7NaaccrProcessor.NAACCR_EXCHANGE_PROPERTY.REPORT_STYLE_SYNOPTIC_SEGMENTED);
        
        //TODO could/should do more validation here of other metadata
         
        fluentTemplate.to("direct:"+Hl7v2RouteBuilder.NAACCR_ROUTE_ID)
                .withBody(encodedTestMsg)
                .send();

        mockResult.assertIsSatisfied();

        String expectedMessage = new String(Base64.getDecoder().decode(mockResult.getExchanges().get(0).getIn().getBody(String.class)));
        assertNotNull(expectedMessage, "response body null");
       
    }

}
