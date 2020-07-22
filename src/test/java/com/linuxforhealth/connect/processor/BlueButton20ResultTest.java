/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.util.Properties;

import com.linuxforhealth.connect.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BlueButton20ResultProcessor} processor
 */
public class BlueButton20ResultTest extends CamelTestSupport {

    private Exchange mockedExchange;
    private BlueButton20ResultProcessor convertR3toR4;
    private Properties props;

    private Exchange createMockExchange() throws IOException {
        Exchange mockedExchange = new DefaultExchange(context);
        mockedExchange.setProperty("timestamp", 1593705267);
        mockedExchange.adapt(ExtendedExchange.class).setFromRouteId("bluebutton-20-rest");
        mockedExchange.setProperty("routeUrl", "http://0.0.0.0:8080/bluebutton/v1/Patient?-19990000002151");
        mockedExchange.setProperty("dataStoreUri", "kafka:FHIR_R4_PATIENT?brokers=localhost:9092");
        mockedExchange.setProperty("dataFormat", "fhir-r4");
        mockedExchange.setProperty("uuid", "cd8c3a57-6fc8-4d33-be18-74f6d5b4cd79");

        // Set the message body to an R3 Patient resource bundle json object
        props = TestUtils.loadProperties("bluebutton.properties");
        String resourceStr = props.getProperty("fhir-r3-patient-bundle");
        Resource resource = FhirContext.forDstu3().newJsonParser().parseResource(Bundle.class, resourceStr);
        mockedExchange.getIn().setBody(resource);

        return mockedExchange;
    }

    /**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() throws IOException {
        mockedExchange = createMockExchange();
        convertR3toR4 = new BlueButton20ResultProcessor();
    }

    /**
     * Tests {@link BlueButton20ResultProcessor#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() throws Exception {
        convertR3toR4.process(mockedExchange);
        String expectedBody = props.getProperty("fhir-r4-patient-bundle");
        String actualBody = mockedExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedBody, actualBody);
    }

    /**
     * Tests {@link BlueButton20ResultProcessor#process(Exchange)} to validate that the message body can be parsed to a resource
     */
    @Test
    public void testMessageParse() throws Exception {
        convertR3toR4.process(mockedExchange);
        String expectedBody = props.getProperty("fhir-r4-patient-bundle");

        // Get the body
        String actualBody = mockedExchange.getIn().getBody(String.class);

        // Parse the actual message body to R4 resource
        org.hl7.fhir.r4.model.Resource actualResource = FhirContext.forR4().newJsonParser()
            .parseResource(org.hl7.fhir.r4.model.Bundle.class, actualBody);

        // Parse the expected message body to R4 resource
        org.hl7.fhir.r4.model.Resource expectedResource = FhirContext.forR4().newJsonParser()
            .parseResource(org.hl7.fhir.r4.model.Bundle.class, expectedBody);

        // Compare a trivial field in the two resources, since we don't have a way to compare 2 class instances
        Assertions.assertEquals(expectedResource.getId(), actualResource.getId());
    }
}
