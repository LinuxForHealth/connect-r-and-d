package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LFHKafkaConsumer;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.processor.validation.SchemaValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Tests {@link CcdRouteBuilder}
 */
public class CcdRouteTest extends RouteTestSupport{

    private MockEndpoint mockResult;
    private MockEndpoint mockErrorResult;

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return context -> {
            context.addRoutes(new CcdRouteBuilder());
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:error")
                    .to("mock:error-result");
                }
            });
        };
    }

    /**
     * Overridden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        super.configureContext();

        mockResult = mockProducerEndpoint(CcdRouteBuilder.ROUTE_ID,
                LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI,
                "mock:result");

        mockErrorResult = mockProducerEndpoint(CcdRouteBuilder.ROUTE_ID,
                LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI,
                "mock:error-result");
    }

    /**
     * Tests the route where processing completes successfully.
     */
    @Test
    void testRoute() throws Exception {

        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("ccd", "SampleCCDDocument.xml"))
                .replaceAll(System.lineSeparator(), "");

        String expectedMessage = Base64.getEncoder().encodeToString(testMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:HL7-V3_CCD?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "HL7-V3");
        mockResult.expectedPropertyReceived("messageType", "CCD");
        mockResult.expectedPropertyReceived("routeId", "ccd");

        fluentTemplate.to("http://0.0.0.0:8080/ccd")
                .withBody(testMessage)
                .send();

        mockResult.assertIsSatisfied();
    }

    /**
     * Tests the route when a validation error occurs.
     */
    @Test
    void testRouteValidationError() throws Exception {

        String testMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("ccd", "SampleCCDDocument.xml"))
                .replaceAll(System.lineSeparator(), "").replaceAll("ClinicalDocument", "InvalidDocument");

        mockErrorResult.expectedMessageCount(1);

        fluentTemplate.to("http://0.0.0.0:8080/ccd")
                .withBody(testMessage)
                .send();

        mockErrorResult.assertIsSatisfied();
    }
}
