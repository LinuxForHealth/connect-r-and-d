/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import ca.uhn.fhir.context.FhirContext;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

@Disabled
public class FhirR4ToAcdProcessorTest extends CamelTestSupport {
	
	private Exchange mockedExchange;
	private FhirR4ToAcdProcessor fhirR4ToAcdProcessor;
	private Properties props;

	public FhirR4ToAcdProcessorTest() { }
	
	private Exchange createMockExchange() throws IOException {
		Exchange mockedExchange = new DefaultExchange(context);
		mockedExchange.setProperty("timestamp", 1593705267);
        mockedExchange.adapt(ExtendedExchange.class).setFromRouteId("bluebutton-20-rest");
        mockedExchange.setProperty("routeUri", "direct:fhir-r4-to-acd");
        mockedExchange.setProperty("dataStoreUri", "kafka:FHIR_R4_DOCUMENTREFERENCE?brokers=localhost:9094");
        mockedExchange.setProperty("dataFormat", "fhir-r4");
        mockedExchange.setProperty("uuid", "cd8c3a57-6fc8-4d33-be18-74f6d5b4cd79");
        
        props = TestUtils.loadProperties("acd.properties");
        String kafkaMsgStr = props.getProperty("kafka-msg-fhir-r4-document-reference");
        
        JSONObject jsonObj = new JSONObject(kafkaMsgStr);
		
		JSONObject dataObj = jsonObj.getJSONObject("data");
		
		DocumentReference docRef = FhirContext.forR4().newJsonParser()
				.parseResource(DocumentReference.class, dataObj.toString());
		
		mockedExchange.getIn().setBody(docRef);
        
		return mockedExchange;
	}
	
	/**
     * Configures a mocked exchange fixture
     */
    @BeforeEach
    public void beforeEach() throws IOException {
        mockedExchange = createMockExchange();
        fhirR4ToAcdProcessor = new FhirR4ToAcdProcessor();
    }

    /**
     * Tests {@link FhirR4ToAcdProcessor#process(Exchange)} to validate that the message body matches an expected result
     */
    @Test
    public void testProcess() throws Exception {
    	
    	fhirR4ToAcdProcessor.process(mockedExchange);
        String expectedBody = props.getProperty("fhir-r4-document-reference");
        
        DocumentReference docRef = mockedExchange.getIn().getBody(DocumentReference.class);
        String actualBody = FhirContext.forR4().newJsonParser().encodeResourceToString(docRef);
        
        Assertions.assertEquals(expectedBody, actualBody);
    }

}
