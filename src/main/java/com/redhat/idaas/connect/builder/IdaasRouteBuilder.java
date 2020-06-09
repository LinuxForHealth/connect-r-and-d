package com.redhat.idaas.connect.builder;

import com.redhat.idaas.connect.configuration.EndpointUriBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * Base class for iDAAS Route Builder implementations.
 * Provides convenience methods for resolving application property values and route generation.
 * This class is concrete rather than abstract due to Camel's route scanning mechanism.
 */
public class IdaasRouteBuilder extends RouteBuilder {

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
