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

    public CamelContextSupport(CamelContext context) {
        this.context = context;
    }
}
