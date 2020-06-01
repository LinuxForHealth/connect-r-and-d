package com.redhat.idaas.connect.routes;

import com.redhat.idaas.connect.configuration.PropertyParser;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Parametrized Test cases for iDAAS-Connect generated routes.
 */
public class RouteValidationTest extends CamelTestSupport {

    private static final String PROPERTY_FILE_DIRECTORY = "route-validation-tests/";

    /**
     * Loads a property file from the class path.
     *
     * @param propertyFileName The property file name
     * @return {@link Properties} object
     * @throws IOException if an error occurs reading the properties file from disk.
     */
    private Properties loadProperties(String propertyFileName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertyFileName)) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Registers a bean in the Camel Registry
     *
     * @param beanName  The bean name used for lookups
     * @param className The bean's class name
     * @throws ReflectiveOperationException if an error occurs instantiating a class.
     */
    private void registerCamelBean(String beanName, String className) throws ReflectiveOperationException {
        Constructor<?> componentConstructor = Class.forName(className).getConstructor();
        context.getRegistry().bind(beanName, componentConstructor.newInstance());
    }

    /**
     * Loads the test case's route configuration from properties into a Camel context.
     *
     * @param propertyFilePath The path t properties used for the test case
     * @return propertyParser a Property Parser instance
     * @throws Exception if an error occurs during route generation
     */
    public PropertyParser setupRoutes(String propertyFilePath) throws Exception {
        Properties properties = loadProperties(propertyFilePath);
        PropertyParser propertyParser = new PropertyParser(properties);

        for (Entry<String, String> entry : propertyParser.getIdaasComponents().entrySet()) {
            registerCamelBean(entry.getKey(), entry.getValue());
        }

        for (RouteBuilder routeBuilder : propertyParser.getIdaasRouteDefinitions()) {
            routeBuilder.addRoutesToCamelContext(context);
        }

        return propertyParser;
    }

    /**
     * Returns a bean from the Camel Registry
     *
     * @param beanName The bean's lookup name
     * @return The bean
     */
    Object getCamelBean(String beanName) {
        return context().getRegistry().lookupByName(beanName);
    }

    /**
     * Looks up an {@link Endpoint} by URI.
     *
     * @param endpointUri The Camel Endpoint URI
     * @return The {@link Endpoint} if found, otherwise returns null
     */
    Endpoint getEndpoint(String endpointUri) {
        return context().getEndpoint(endpointUri);
    }

    /**
     * Provides arguments to {@link RouteValidationTest#testRouteGeneration(String, boolean, String, String...)}
     *
     * @return {@link Stream} of arguments
     */
    static Stream<Arguments> routeGenerationProvider() {
        return Stream.of(
                Arguments.arguments("hl7-mllp",
                        true,
                        "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder",
                        new String[]{"stub://hl7-stub"})
        );
    }

    /**
     * Validates that property based routes are generated without issue and with the expected configurations
     *
     * @param routeId            The expected route id
     * @param hasRegisteredBeans true if the route is expected to have registered beans
     * @param consumerUri        The expected consumer uri
     * @param producerUris       The expected producer uris
     */
    @ParameterizedTest
    @MethodSource("routeGenerationProvider")
    void testRouteGeneration(String routeId,
                             boolean hasRegisteredBeans,
                             String consumerUri,
                             String... producerUris) throws Exception {
        String propertyFilePath = PROPERTY_FILE_DIRECTORY + routeId + ".properties";
        PropertyParser propertyParser = setupRoutes(propertyFilePath);

        Assertions.assertEquals(hasRegisteredBeans, propertyParser.getIdaasComponents().keySet().size() > 0);

        propertyParser.getIdaasComponents()
                .keySet()
                .forEach(componentKey -> Assertions.assertNotNull(getCamelBean(componentKey)));

        Assertions.assertNotNull(getEndpoint(consumerUri));

        Arrays.stream(producerUris)
                .forEach(uri -> Assertions.assertNotNull(getEndpoint(uri)));
    }
}
