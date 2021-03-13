/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import org.junit.jupiter.api.Assertions;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.util.StringUtils;
import com.linuxforhealth.connect.support.TestUtils;

/**
 * Tests {@link Hl7v2RouteBuilder}
 */
public class Hl7v2FHIRConverterRouteBuilderTest extends RouteTestSupport {


  private MockEndpoint mockFHIREndpoint;
  private MockEndpoint mockStoreAndNotify;
  private MockEndpoint mockErrorEndpoint;


  Properties props = null;

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new Hl7v2RouteBuilder();
  }

  /**
   * Overridden to register beans, apply advice, and register a mock endpoint
   * 
   * @throws Exception if an error occurs applying advice
   */
  @BeforeEach
  @Override
  protected void configureContext() throws Exception {
    HL7MLLPNettyEncoderFactory hl7encoder = new HL7MLLPNettyEncoderFactory();
    HL7MLLPNettyDecoderFactory hl7decoder = new HL7MLLPNettyDecoderFactory();

    context.getRegistry().bind("hl7encoder", hl7encoder);
    context.getRegistry().bind("hl7decoder", hl7decoder);


    mockFHIREndpoint = mockProducerEndpoint(Hl7v2RouteBuilder.ROUTE_ID,
        FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_URI, "mock:toExternalFhirServers");
    mockStoreAndNotify = mockProducerEndpoint(Hl7v2RouteBuilder.ROUTE_ID,
        LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI, "mock:storeAndNotify");
    mockErrorEndpoint = mockProducerEndpoint(Hl7v2RouteBuilder.ROUTE_ID,
        LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI, "mock:error");

    super.configureContext();
  }


  @Override
  protected Properties useOverridePropertiesWithPropertiesComponent() {

    props = super.useOverridePropertiesWithPropertiesComponent();

    return props;

  }


  @Test
  void test_hl7_fhir_convert_Route_enabled() throws Exception {
    props.put("lfh.connect.hl7-v2.convertToFhir", "true");
    String testMessage = context.getTypeConverter()
        .convertTo(String.class, TestUtils.getMessage("hl7", "ADT_A01.txt"))
        .replace(System.lineSeparator(), "\r");


    mockStoreAndNotify.expectedMessageCount(2);
    mockFHIREndpoint.expectedMessageCount(1);
    mockStoreAndNotify.expectedPropertyReceived("dataFormat", "HL7-V2");
    mockStoreAndNotify.expectedPropertyReceived("messageType", "ADT");
    mockStoreAndNotify.expectedPropertyReceived("routeId", "hl7-v2");
    fluentTemplate
        .to("netty:tcp://localhost:2576?sync=true&encoders=#hl7encoder&decoders=#hl7decoder")
        .withBody(testMessage).send();

    Assertions.assertFalse(mockFHIREndpoint.getExchanges().isEmpty(), "mockFHIREndpoint should not be empty");
    String actualJson = (mockFHIREndpoint.getExchanges().get(0).getIn().getBody(String.class));
    Assertions.assertTrue(StringUtils.contains(actualJson, "\"resourceType\":\"Bundle\""),
        "Output not expected");

    Assertions.assertFalse(mockStoreAndNotify.getExchanges().isEmpty(),
        "mockStoreAndNotify should not be empty");
    String fhirResource = mockStoreAndNotify.getExchanges().get(1).getIn().getBody(String.class);
    Assertions.assertTrue(StringUtils.contains(fhirResource, "\"resourceType\":\"Bundle\""),
        "Output not expected");

    assertMockEndpointsSatisfied();



  }




  @Test
  void test_hl7_fhir_convert_Route_not_enabled() throws Exception {
    props.put("lfh.connect.hl7-v2.convertToFhir", "false");
    String testMessage = context.getTypeConverter()
        .convertTo(String.class, TestUtils.getMessage("hl7", "ADT_A01.txt"))
        .replace(System.lineSeparator(), "\r");

    String expectedHl7Message =
        Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

    mockStoreAndNotify.expectedMessageCount(1);
    mockFHIREndpoint.expectedMessageCount(0);
    mockStoreAndNotify.expectedBodiesReceived(expectedHl7Message);

    fluentTemplate
        .to("netty:tcp://localhost:2576?sync=true&encoders=#hl7encoder&decoders=#hl7decoder")
        .withBody(testMessage).send();

    Assertions.assertTrue(mockFHIREndpoint.getExchanges().isEmpty(), "mockFHIREndpoint should  be empty");

    assertMockEndpointsSatisfied(); // Verifies if input is equal to output



  }



  @Test // Passing unsupported HL7 message type will result in IllegalStateException
  void test_hl7_fhir_convert_Route_enabled_exception_encountered() throws Exception {
    props.put("lfh.connect.hl7-v2.convertToFhir", "true");
    String testMessage = context.getTypeConverter()
        .convertTo(String.class, TestUtils.getMessage("hl7", "ADT_A28.txt")) // unsupported message
                                                                             // for HL7 to FHIR
                                                                             // conversion
        .replace(System.lineSeparator(), "\r");

    String expectedHl7Message =
        Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

    mockFHIREndpoint = mockProducerEndpoint(Hl7v2RouteBuilder.ROUTE_ID,
        FhirR4RouteBuilder.EXTERNAL_FHIR_ROUTE_URI, "mock:toExternalFhirServers");

    mockStoreAndNotify.expectedMessageCount(1);
    mockFHIREndpoint.expectedMessageCount(0);
    mockStoreAndNotify.expectedBodiesReceived(expectedHl7Message);

    fluentTemplate
        .to("netty:tcp://localhost:2576?sync=true&encoders=#hl7encoder&decoders=#hl7decoder")
        .withBody(testMessage).send();

    Assertions.assertTrue(mockFHIREndpoint.getExchanges().isEmpty(), "mockFHIREndpoint should  be empty");

    assertMockEndpointsSatisfied(); // Verifies if input is equal to output



  }
}
