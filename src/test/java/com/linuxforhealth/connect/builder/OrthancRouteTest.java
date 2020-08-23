package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LinuxForHealthAssertions;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * Tests {@link FhirR4RouteBuilder}
 */
public class OrthancRouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new OrthancRouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.orthanc.uri", "direct:http://0.0.0.0:9090/orthanc/instances");
        props.setProperty("lfh.connect.orthanc_server.uri", "http://localhost:8042/instances");
        return props;
    }

    /**
     * Overriden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {

        setProducerResponse(OrthancRouteBuilder.ROUTE_ID,
                OrthancRouteBuilder.ORTHANC_PRODUCER_POST_ID,
                "orthanc",
                "post-response.json");

        setProducerResponse(OrthancRouteBuilder.ROUTE_ID,
                OrthancRouteBuilder.ORTHANC_PRODUCER_GET_ID,
                "orthanc",
                "get-response.json");

        mockProducerEndpointById(OrthancRouteBuilder.ROUTE_ID,
                OrthancRouteBuilder.ORTHANC_PRODUCER_STORE_NOTIFY_ID,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Tests {@link OrthancRouteBuilder#ROUTE_ID}
     * @throws Exception
     */
    @Test
    void testRoute() throws Exception {
        String expectedMessage = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage("orthanc", "result.json"));

        expectedMessage = Base64.getEncoder().encodeToString(expectedMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:DICOM_IMAGE?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "DICOM");
        mockResult.expectedPropertyReceived("messageType", "IMAGE");
        mockResult.expectedPropertyReceived("routeId", "orthanc-post");

        File inputFile = TestUtils.getMessage("orthanc", "image-00020.dcm");
        byte[] inputMessage = Files.readAllBytes(Paths.get(inputFile.toURI()));
        fluentTemplate.to("{{lfh.connect.orthanc.uri}}")
                .withBody(inputMessage)
                .request();

        mockResult.assertIsSatisfied();

        Exchange mockExchange = mockResult.getExchanges().get(0);

        String expectedRouteUri = "direct://http://0.0.0.0:9090/orthanc/instances";
        String actualRouteUri = mockExchange.getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
