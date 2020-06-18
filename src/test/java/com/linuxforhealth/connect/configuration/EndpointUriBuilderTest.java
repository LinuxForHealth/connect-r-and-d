/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.configuration;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Tests {@link EndpointUriBuilder}
 */
class EndpointUriBuilderTest {

    private Properties testProperties;
    private EndpointUriBuilder uriBuilder;

    /**
     * Configures the {@link EndpointUriBuilder} fixture with properties from file
     */
    @BeforeEach
    void beforeEach() throws IOException {
        testProperties = new Properties();

        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("test-application.properties")) {
            testProperties.load(inputStream);
        }

        uriBuilder = new EndpointUriBuilder(testProperties);
    }

    /**
     * Tests {@link EndpointUriBuilder#getHl7V2MllpUri()}
     */
    @Test
    void testGetHl7V2MllpUri() {
        String expectedUri = "netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder";
        Assertions.assertEquals(expectedUri, uriBuilder.getHl7V2MllpUri());
    }

    /**
     * Tests {@link EndpointUriBuilder#getFhirR4RestUri()}
     */
    @Test
    void testGetFhirR4RestUri() {
        String expectedUri = "http://localhost:8080/fhir/r4";
        Assertions.assertEquals(expectedUri, uriBuilder.getFhirR4RestUri());
    }

    /**
     * Tests {@link EndpointUriBuilder#getExternalFhirR4RestUri} for a null return
     */
    @Test
    void testGetExternalFhirR4RestUriForNullReturn() {
        Assertions.assertNull(uriBuilder.getExternalFhirR4RestUri("Patient"));
        Assertions.assertNull(uriBuilder.getExternalFhirR4RestUri(null));
    }

    /**
     * Tests {@link EndpointUriBuilder#getExternalFhirR4RestUri} for a null return
     */
    @Test
    void testGetExternalFhirR4RestUri() {

        testProperties.put("linuxforhealth.connect.endpoint.external_fhir_r4_rest.baseUri", "https://fhir.md.com/api/fhir/v4");
        testProperties.put("linuxforhealth.connect.endpoint.external_fhir_r4_rest.options", "foo=bar");
        uriBuilder = new EndpointUriBuilder(testProperties);

        String expectedUri = "https://fhir.md.com/api/fhir/v4/patient?foo=bar";
        Assertions.assertEquals(expectedUri, uriBuilder.getExternalFhirR4RestUri("patient"));
    }

    /**
     * Tests {@link EndpointUriBuilder#getDataStoreUri} 
     */
    @Test
    void testGetDataStoreUri() {
        String expectedUri = "kafka:FHIR_R4_PATIENT?brokers=localhost:9092";
        Assertions.assertEquals(expectedUri, uriBuilder.getDataStoreUri("FHIR_R4_PATIENT"));
    }
}
