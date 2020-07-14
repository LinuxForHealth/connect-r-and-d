/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.nats.NatsComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parametrized Test cases for Linux For Health Connect generated routes.
 */
class RouteGenerationTest extends CamelTestSupport {

    private static final String PROPERTY_FILE_DIRECTORY = "route-validation-tests/";

    /**
     * Registers required components within the Camel context
     */
    @BeforeEach
    void beforeEach() {
        Map<String, Object> allComponents = new HashMap<>();
        allComponents.put("hl7encoder", new HL7MLLPNettyEncoderFactory());
        allComponents.put("hl7decoder", new HL7MLLPNettyDecoderFactory());
        allComponents.put("kafka", new KafkaComponent());
        allComponents.put("nats", new NatsComponent());

        Registry registry = context().getRegistry();
        allComponents.forEach(registry::bind);
    }

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
     * Provides arguments to {@link RouteGenerationTest#testRouteGeneration}
     * Arguments include:
     * <ul>
     *     <li>The Linux for Health's Connect Route test subject</li>
     *     <li>The route's supporting property file</li>
     *     <li>The expected consumer URI</li>
     *     <li>The expected producer URIs</li>
     * </ul>
     *
     * @return {@link Stream} of arguments
     */
    static Stream<Arguments> routeGenerationProvider() {
        return Stream.of(
                Arguments.arguments(
                        new Hl7v2MllpRouteBuilder(),
                        PROPERTY_FILE_DIRECTORY.concat("hl7-v2-mllp.properties"),
                        "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder",
                        new String[]{
                                "mock:kafka?brokers=localhost:9092",
                                "mock:nats:idaas-data?servers=localhost:4222"
                        },
                        new HashMap<String, String>()
                ),
                Arguments.arguments(
                        new FhirR4RestRouteBuilder(),
                        PROPERTY_FILE_DIRECTORY.concat("fhir-r4-rest.properties"),
                        "http://localhost:8080/fhir/r4",
                        new String[]{
                                "mock:kafka?brokers=localhost:9092",
                                "mock:nats:idaas-data?servers=localhost:4222"
                        }
                ),
                Arguments.arguments(
                        new BlueButton20RestRouteBuilder(),
                        PROPERTY_FILE_DIRECTORY.concat("bluebutton-20-rest.properties"),
                        "http://0.0.0.0:8080/bluebutton/v1",
                        new String[]{
                                "mock:https://sandbox.bluebutton.cms.gov/v1/fhir/",
                                "mock:kafka?brokers=localhost:9092",
                                "mock:nats:idaas-data?servers=localhost:4222"
                        }
                ),
                Arguments.arguments(
                        new FhirR4ToAcdRouteBuilder(),
                        PROPERTY_FILE_DIRECTORY.concat("fhir-to-acd.properties"),
                        "direct:fhir-r4-to-acd",
                        new String[]{
                        		"mock:kafka?brokers=localhost:9092",
                                "mock:nats:idaas-data?servers=localhost:4222"
                        }
                )
        );
    }

    /**
     * Parameterized test used to validate Linux for Health Connect Processing Routes
     *
     * @param routeBuilder         The idaas connect {@link org.apache.camel.builder.RouteBuilder} instance
     * @param propertyFileName     The name of the route's supporting property file.
     * @param expectedConsumerUri  The expected route consumer URI
     * @param expectedProducerUris The expected route producer URIs
     * @throws Exception if an error occurs reading properties or configuring the camel context
     */
    @ParameterizedTest
    @MethodSource("routeGenerationProvider")
    void testRouteGeneration(LinuxForHealthRouteBuilder routeBuilder,
                             String propertyFileName,
                             String expectedConsumerUri,
                             String[] expectedProducerUris) throws Exception {

        Properties properties = loadProperties(propertyFileName);
        EndpointUriBuilder endpointUriBuilder = new EndpointUriBuilder(properties);
        context().getRegistry().bind(EndpointUriBuilder.BEAN_NAME, endpointUriBuilder);

        getMandatoryEndpoint(expectedConsumerUri);
        Arrays.stream(expectedProducerUris).forEach(this::getMockEndpoint);
    }
}
