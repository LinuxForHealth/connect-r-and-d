package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.LinuxForHealthAssertions;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * Tests {@link ExampleRouteBuilder}
 */
public class HelloWorldRouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new ExampleRouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.example.uri", "direct:http://0.0.0.0:9090/hello-world");
        return props;
    }

    /**
     * Overriden to register beans, apply advice, and register a mock endpoint
     * @throws Exception if an error occurs applying advice
     */
    @BeforeEach
    @Override
    protected void configureContext() throws Exception {

        mockProducerEndpointById(ExampleRouteBuilder.HELLO_WORLD_ROUTE_ID,
                ExampleRouteBuilder.HELLO_WORLD_PRODUCER_ID,
                "mock:result");

        super.configureContext();

        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Tests {@link ExampleRouteBuilder#HELLO_WORLD_ROUTE_ID}
     * @throws Exception
     */
    @Test
    void testRoute() throws Exception {
        String expectedMessage = "Hello World! It's me.";
        expectedMessage = Base64.getEncoder().encodeToString(expectedMessage.getBytes(StandardCharsets.UTF_8));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedMessage);
        mockResult.expectedPropertyReceived("dataStoreUri", "kafka:EXAMPLE_TEXT?brokers=localhost:9094");
        mockResult.expectedPropertyReceived("dataFormat", "EXAMPLE");
        mockResult.expectedPropertyReceived("messageType", "TEXT");
        mockResult.expectedPropertyReceived("routeId", "hello-world");

        fluentTemplate.to("{{lfh.connect.example.uri}}")
                .withHeader("name", "me")
                .request();

        mockResult.assertIsSatisfied();

        Exchange mockExchange = mockResult.getExchanges().get(0);

        String expectedRouteUri = "direct://http://0.0.0.0:9090/hello-world";
        String actualRouteUri = mockExchange.getProperty("routeUri", String.class);
        LinuxForHealthAssertions.assertEndpointUriSame(expectedRouteUri, actualRouteUri);

        Long actualTimestamp = mockExchange.getProperty("timestamp", Long.class);
        Assertions.assertNotNull(actualTimestamp);
        Assertions.assertTrue(actualTimestamp > 0);

        UUID actualUuid = UUID.fromString(mockExchange.getProperty("uuid", String.class));
        Assertions.assertEquals(36, actualUuid.toString().length());
    }
}
