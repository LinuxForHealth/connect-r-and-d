package com.redhat.idaas.connect.builder;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.PropertiesComponent;

/**
 * Base class for iDAAS Route Builder implementations.
 * Provides convenience methods for resolving application property values and route generation.
 * This class is concrete rather than abstract due to Camel's route scanning mechanism.
 */
public class IdaasRouteBuilder extends RouteBuilder {

    private static final String HL7_V2_MLLP_BASE_URI_PROP_KEY = "idaas.connect.endpoint.hl7_v2_mllp.baseUri";
    private static final String HL7_V2_MLLP_OPTIONS_PROP_KEY = "idaas.connect.endpoint.hl7_v2_mllp.options";

    private static final String DATA_STORE_BASE_URI_PROP_KEY = "idaas.connect.endpoint.datastore.baseUri";
    private static final String DATA_STORE_OPTIONS_PROP_KEY = "idaas.connect.endpoint.datastore.options";

    /**
     * @return the HL7 V2 MLLP URI
     */
    public String getHl7V2MllpUri() {
        PropertiesComponent properties = getContext().getPropertiesComponent();

        return properties.resolveProperty(HL7_V2_MLLP_BASE_URI_PROP_KEY).orElse("")
                .concat("?")
                .concat(properties.resolveProperty(HL7_V2_MLLP_OPTIONS_PROP_KEY).orElse(""));
    }

    /**
     * Builds a data store URI
     * @param topicName The data store topic/segment for the URI
     * @return the data store URI
     */
    public String getDataStoreUri(String topicName) {
        PropertiesComponent properties = getContext().getPropertiesComponent();

        return properties
                .resolveProperty(DATA_STORE_BASE_URI_PROP_KEY).orElse("")
                .concat(":")
                .concat(topicName)
                .concat("?")
                .concat(properties.resolveProperty(DATA_STORE_OPTIONS_PROP_KEY).orElse(""));
    }


    @Override
    public void configure() throws Exception {}

}
