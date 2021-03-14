/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Tests {@link CamelContextSupport}
 */
public class CamelContextSupportTest extends CamelTestSupport {

    private CamelContextSupport camelContextSupport;

    /**
     * Overriden to provide property test values for unit testing
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties overrideProperties = new Properties();
        overrideProperties.setProperty("lfh.connect.host", "0.0.0.0");
        overrideProperties.setProperty("lfh.connect.port", "8090");
        overrideProperties.setProperty("lfh.connect.uri", "http://{{lfh.connect.host}}:{{lfh.connect.port}}/data");
        return overrideProperties;
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        registry.bind("sample-component", SampleComponent.class, new SampleComponent());
    }

    /**
     * Initializes the {@link CamelContextSupport} prior to each test
     */
    @BeforeEach
    public void beforeEach() {
        camelContextSupport = new CamelContextSupport(this.context);
    }

    /**
     * Tests {@link CamelContextSupport#getProperty(String)} where a property is found
     */
    @Test
    void testGetProperties() {
        String expectedValue = "http://0.0.0.0:8090/data";
        String actualValue = camelContextSupport.getProperty("lfh.connect.uri");
        Assertions.assertEquals(expectedValue, actualValue);
    }

    /**
     * Tests {@link CamelContextSupport#getProperty(String)} where a property is not found
     */
    @Test
    void testGetPropertiesNotFound() {
        Assertions.assertThrows(RuntimeException.class,
                () -> camelContextSupport.getProperty("foo"),
                "RuntimeException was not thrown");
    }

    /**
     * Tests {@link CamelContextSupport#getComponent(String, Class)} where a component is found
     */
    @Test
    void testGetComponentByClass() {
        SampleComponent sampleComponent = camelContextSupport.getComponent("sample-component", SampleComponent.class);
        Assertions.assertEquals(SampleComponent.class, sampleComponent.getClass());
    }

    /**
     * Tests {@link CamelContextSupport#getComponent(String, Class)} where a component is not found
     */
    @Test
    void testGetComponentByClassNotFound() {
        SampleComponent sampleComponent = camelContextSupport.getComponent("foo", SampleComponent.class);
        Assertions.assertNull(sampleComponent);
    }

    /**
     * Tests {@link CamelContextSupport#getComponent(String)} where a component is found
     */
    @Test
    void testGetComponent() {
        Object sampleComponent = camelContextSupport.getComponent("sample-component");
        Assertions.assertNotNull(sampleComponent);
    }

    /**
     * Tests {@link CamelContextSupport#getComponent(String)} where a component is not found
     */
    @Test
    void testGetComponentNotFound() {
        Object sampleComponent = camelContextSupport.getComponent("foo");
        Assertions.assertNull(sampleComponent);
    }

    private class SampleComponent {}

}
