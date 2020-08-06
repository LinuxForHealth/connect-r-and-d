package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LinuxForHealthAssertions;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Tests {@link FhirR4RestRouteBuilder}
 */
public class FhirR4RestRouteBuilderTest extends RouteBuilderTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new FhirR4RestRouteBuilder();
    }

    /**
     * Overriden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        applyAdvice(
                FhirR4RestRouteBuilder.ROUTE_ID,
                LinuxForHealthDirectRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                "mock:result"
        );

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    @Test
    void testRoute() throws Exception {
        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("fhir", "fhir-r4-patient-bundle.json"))
                .replace("\n", "");

        String expectedMessage = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        // the camel hl7 data format removes trailing delimiters from segments and fields
        // test files do not include trailing delimiters to simplify test assertions
        // the data format will include a terminating carriage return, \r, which is translated above from a new line \n
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:FHIR-R4_PATIENT?brokers=localhost:9092");
        mockResult.expectedPropertyReceived("dataFormat", "FHIR-R4");
        mockResult.expectedPropertyReceived("messageType", "PATIENT");
        mockResult.expectedPropertyReceived("routeId", "fhir-r4-rest");

        producerTemplate.sendBody("{{lfh.connect.fhir_r4_rest.uri}}/Patient", testMessage);

        mockResult.assertIsSatisfied();

        String expectedRouteUri = "jetty:http://0.0.0.0:8080/fhir/r4/Patient?httpMethodRestrict=POST";
        String actualRouteUri = mockResult.getExchanges().get(0).getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Exchange mockExchange = mockResult.getExchanges().get(0);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
