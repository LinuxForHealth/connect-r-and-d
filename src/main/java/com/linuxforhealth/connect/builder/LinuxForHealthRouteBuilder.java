/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * Base class for Linux for Health Route Builder implementations.
 * Provides convenience methods for resolving application property values and route generation.
 * This class is concrete rather than abstract due to Camel's route scanning mechanism.
 */
public class LinuxForHealthRouteBuilder extends RouteBuilder {

    /**
     * @return {@link EndpointUriBuilder} used to build endpoint uris for consumers and producers
     */
    public EndpointUriBuilder getEndpointUriBuilder() {
        return getContext()
                .getRegistry()
                .lookupByNameAndType(EndpointUriBuilder.BEAN_NAME, EndpointUriBuilder.class);
    }

    @Override
    public void configure() {}
}
