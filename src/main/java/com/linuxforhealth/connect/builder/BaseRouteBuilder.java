package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.builder.RouteBuilder;

import java.util.Arrays;

/**
 * Base class for Linux for Health {@link RouteBuilder} implementations
 */
public abstract class BaseRouteBuilder extends RouteBuilder {

    /**
     * @return the route property namespace. Example: lfh.connect.hl7_v2_mllp.
     */
    protected abstract String getRoutePropertyNamespace();

    /**
     * Validates that all required properties are present within the {@link CamelContextSupport}
     * @throws RuntimeException if a required property is missing
     */
    protected void validateRequiredProperties() {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String propertyNamespace = getRoutePropertyNamespace();

        if (propertyNamespace == null || propertyNamespace.trim().equals("")) {
            throw new RuntimeException("route property namespace is not populated");
        }

        Arrays.asList(
                propertyNamespace + ".uri",
                propertyNamespace + ".dataFormat",
                propertyNamespace + ".messageType"
        ).forEach(ctxSupport::getProperty);
    }

    /**
     * Builds a Linux for Health Route, using Camel's Java DSL.
     * @param routePropertyNamespace The property namespace for the route.
     */
    protected abstract void buildRoute(String routePropertyNamespace);

    /**
     * Validates required properties and then builds the data processing route
     */
    @Override
    public void configure() {
        validateRequiredProperties();
        buildRoute(getRoutePropertyNamespace());
    }
}
