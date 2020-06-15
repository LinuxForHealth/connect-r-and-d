/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.LinuxForHealthProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

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

    public void process() {}

    /**
     * Utility method to set common JSON message attributes from within a processor
     */
    public void setCommonMeta(Exchange exchange, JSONObject meta) {
        meta.put("routeId", exchange.getFromRouteId());
        meta.put("uuid", exchange.getIn().getHeader("uuid", String.class));
        meta.put("routeUrl", exchange.getIn().getHeader("routeUrl", String.class));
        meta.put("dataFormat", exchange.getIn().getHeader("dataFormat", String.class));
    }
}
