/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Base class for Linux for Health Processor implementations.
 * Provides convenience methods for resolving application property values and route generation.
 */
public abstract class LinuxForHealthProcessor implements Processor {

    /**
     * @return {@link EndpointUriBuilder} used to build endpoint uris for consumers and producers
     */
    public EndpointUriBuilder getEndpointUriBuilder(Exchange exchange) {
        return exchange.getContext()
                .getRegistry()
                .lookupByNameAndType(EndpointUriBuilder.BEAN_NAME, EndpointUriBuilder.class);
    }
}
