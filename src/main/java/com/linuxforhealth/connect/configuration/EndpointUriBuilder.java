package com.linuxforhealth.connect.configuration;

import java.util.Properties;

/**
 * Builds Camel Endpoint URIs from application properties
 */
public final class EndpointUriBuilder {
    public static final String BEAN_NAME = "endpointUriBuilder";

    private static final String HL7_V2_MLLP_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.hl7_v2_mllp.baseUri";
    private static final String HL7_V2_MLLP_OPTIONS_PROP_KEY = "linuxforhealth.connect.endpoint.hl7_v2_mllp.options";

    private static final String FHIR_R4_REST_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.fhir_r4_rest.baseUri";

    private static final String FHIR_R4_EXTERNAL_REST_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.external_fhir_r4_rest.baseUri";
    private static final String FHIR_R4_EXTERNAL_REST_OPTIONS_PROP_KEY = "linuxforhealth.connect.endpoint.external_fhir_r4_rest.options";

    private static final String DATA_STORE_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.datastore.baseUri";
    private static final String DATA_STORE_OPTIONS_PROP_KEY = "linuxforhealth.connect.endpoint.datastore.options";

    private static final String MESSAGING_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.messaging.baseUri";

    private final Properties appProperties;

    /**
     * Builds the HL7 V2 MLLP URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.hl7_v2_mllp.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.hl7_v2_mllp.options</li>
     * </ul>
     * @return the HL7 V2 MLLP URI
     */
    public String getHl7V2MllpUri() {
        return appProperties.getProperty(HL7_V2_MLLP_BASE_URI_PROP_KEY)
                .concat("?")
                .concat(appProperties.getProperty(HL7_V2_MLLP_OPTIONS_PROP_KEY, ""));
    }

    /**
     * Builds the FHIR R4 Rest URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.fhir_r4_rest.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.fhir_r4_rest.options</li>
     * </ul>
     * @return the FHIR R4 Rest URI
     */
    public String getFhirR4RestUri() {
        return appProperties.getProperty(FHIR_R4_REST_BASE_URI_PROP_KEY);
    }

    /**
     * Builds the external FHIR R4 Rest URI for a resource using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.external_fhir_r4_rest.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.external_fhir_r4_rest.options</li>
     * </ul>
     * @param resourceType the FHIR resource type
     * @return the FHIR R4 Rest URI
     */
    public String getExternalFhirR4RestUri(String resourceType) {
        String externalFhirBaseUri = appProperties.getProperty(FHIR_R4_EXTERNAL_REST_BASE_URI_PROP_KEY);

        if (externalFhirBaseUri == null || resourceType == null) {
            return null;
        }

        return externalFhirBaseUri
                .concat("/")
                .concat(resourceType)
                .concat("?")
                .concat(appProperties.getProperty(FHIR_R4_EXTERNAL_REST_OPTIONS_PROP_KEY));
    }

    /**
     * Builds a data store URI
     * @param topicName The data store topic/segment for the URI
     * @return the data store URI
     */
    public String getDataStoreUri(String topicName) {
        return appProperties.getProperty(DATA_STORE_BASE_URI_PROP_KEY)
                .concat(":")
                .concat(topicName)
                .concat("?")
                .concat(appProperties.getProperty(DATA_STORE_OPTIONS_PROP_KEY));
    }

    /**
     * Builds a messaging URI
     * @return the messaging URI
     */
    public String getMessagingUri() {
        return appProperties.getProperty(MESSAGING_BASE_URI_PROP_KEY);
    }

    public EndpointUriBuilder(Properties appProperties) {
        this.appProperties = appProperties;
    }
}
