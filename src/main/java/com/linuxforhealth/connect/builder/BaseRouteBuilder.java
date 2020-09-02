/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.builder.RouteBuilder;

import java.util.Arrays;

/**
 * Base class for Linux for Health {@link RouteBuilder} implementations.
 * Provides basic property validation and error handling.
 * Route implementations are provided by overriding <code>BaseRouteBuilder.buildRoute</code>.
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
                propertyNamespace + ".dataformat",
                propertyNamespace + ".messagetype"
        ).forEach(ctxSupport::getProperty);
    }

    /**
     * Builds a Linux for Health Route, using Camel's Java DSL.
     * @param routePropertyNamespace The property namespace for the route.
     */
    protected abstract void buildRoute(String routePropertyNamespace);

    /**
     * Configures the Camel {@link org.apache.camel.builder.ErrorHandlerBuilder} for the context.
     * The default implementation uses the dead letter channel error handler, directing failed messages
     * to {@link LinuxForHealthRouteBuilder#ERROR_CONSUMER_URI}
     */
    protected void configureErrorHandling() {
        errorHandler(deadLetterChannel(LinuxForHealthRouteBuilder.ERROR_CONSUMER_URI));
    }

    /**
     * Provides base route processing and configuration
     */
    @Override
    public final void configure() {
        configureErrorHandling();
        validateRequiredProperties();
        buildRoute(getRoutePropertyNamespace());
    }
}
