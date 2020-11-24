package com.linuxforhealth.connect.builder;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linuxforhealth.connect.support.TestUtils;

import ca.uhn.fhir.context.FhirContext;

public class FhirR4ToAcdRouteTest extends RouteTestSupport {

	private MockEndpoint mockResult;
	
	 @Override
	    protected RoutesBuilder createRouteBuilder() throws Exception {
	       return new FhirR4ToAcdRouteBuilder();
	   }

    /**
     * Configures mock responses and endpoints for route testing
     * @throws Exception
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
    	
    	// Redirect message route from "direct:acd-analyze" to "mock:result"
    	mockResult = mockProducerEndpoint(FhirR4ToAcdRouteBuilder.FHIR_R4_TO_ACD_ROUTE_ID, "direct:acd-analyze", "mock:result");
    	super.configureContext();
    }
    
    @Test
    void testRoute() throws Exception {
    	
    	mockResult.expectedMessageCount(1);
    	mockResult.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
    	
    	String documentReference = context.getTypeConverter()
        		.convertTo(String.class, TestUtils.getMessage("fhir-acd", "DocumentReference.json"));
    	
    	Assertions.assertNotNull(documentReference, "Failed to load DocumentReference.json route test input");
    	
    	IBaseResource resource = FhirContext.forR4().newJsonParser().parseResource(documentReference);
    	
    	Assertions.assertEquals(resource.fhirType(), "DocumentReference");
        
        fluentTemplate.to(FhirR4ToAcdRouteBuilder.FHIR_R4_TO_ACD_CONSUMER_URI)
        	.withBody(resource)
        	.send();
             
        mockResult.assertIsSatisfied();
      
        String expectedOutMsgBody = context
      		.getTypeConverter()
      		.convertTo(String.class, TestUtils.getMessage("fhir-acd", "output.txt"));
        
        String receivedMsgBody = mockResult.getReceivedExchanges().get(0).getIn().getBody(String.class);
      
        Assertions.assertEquals(expectedOutMsgBody, receivedMsgBody.trim());

    }

}