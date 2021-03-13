/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.CamelContext;

/**
 * Provides utility/convenience methods for {@link CamelContext} related operations.
 */
public class CamelContextSupport {

    private CamelContext context;

    /**
     * Lookups a property in the camel context, returning the property value.
     * This method is typically used in contexts, such as the REST DSL, where simple expressions are not
     * available.
     * @param propertyName The property name/key to lookup.
     * @return The property value
     * @raises {@link RuntimeException} if the property is not found
     */
    public String getProperty(String propertyName) {
        return context
                .getPropertiesComponent()
                .resolveProperty(propertyName)
                .orElseThrow(() -> new RuntimeException("property " + propertyName + " not found"));
    }
    
    /**
     * Returns a component/bean from the Camel Registry.
     * @param componentName The component name
     * @param componentType The component type
     * @return The component instance
     */
    public <T> T getComponent(String componentName, Class<T> componentType) {
        return context.getRegistry()
                .lookupByNameAndType(componentName, componentType);
    }

    /**
     * Returns a component/bean from the Camel Registry.
     * @param componentName The component name
     * @return The component as an Object
     */
    public Object getComponent(String componentName) {
        return context.getRegistry()
                .lookupByName(componentName);
    }

    public CamelContextSupport(CamelContext context) {
        this.context = context;
    }
}
