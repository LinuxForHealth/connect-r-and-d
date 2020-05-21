package com.redhat.idaas.connect.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

/**
 * Tests {@link PropertyParser}
 */
public class PropertyParserTest {

    private static Properties sourceIdaasProperties;

    private PropertyParser propertyParser;

    /**
     * Loads test properties from file
     *
     * @throws IOException for errors reading the property file
     */
    @BeforeAll
    public static void beforeAll() throws IOException {
        sourceIdaasProperties = new Properties();

        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("application.properties")) {
            sourceIdaasProperties.load(inputStream);
        }
    }

    /**
     * Sets up the {@link PropertyParser} prior to each test
     */
    @BeforeEach
    public void beforeEach() {
        propertyParser = new PropertyParser(sourceIdaasProperties);
    }

    /**
     * Tests {@link PropertyParser#PropertyParser(Properties)} to ensure properties are filtered correctly
     */
    @Test
    public void testPropertyFiltering() {
        Assertions.assertNotNull(sourceIdaasProperties.getProperty("some.other.property"));
        Assertions.assertNull(propertyParser.getPropertyValue("some.other.property"));
    }

    /**
     * Tests {@link PropertyParser#PropertyParser(Properties)} and validates component parsing
     */
    @Test
    public void testPropertyParserComponents() {
        Map<String, String> actualComponents = propertyParser.getIdaasComponents();
        Assertions.assertTrue(actualComponents.containsKey("hl7decoder"));
        Assertions.assertTrue(actualComponents.containsKey("hl7encoder"));

        String expectedValue = "org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory";
        Assertions.assertEquals(expectedValue, actualComponents.get("hl7decoder"));

        expectedValue = "org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory";
        Assertions.assertEquals(expectedValue, actualComponents.get("hl7encoder"));
    }
}