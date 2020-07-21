/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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

    private static final String BLUEBUTTON_20_REST_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.baseUri";
    private static final String BLUEBUTTON_20_REST_AUTHORIZE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.authorizeUri";
    private static final String BLUEBUTTON_20_REST_CALLBACK_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.callbackUri";
    private static final String BLUEBUTTON_20_REST_TOKEN_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.tokenUri";
    private static final String BLUEBUTTON_20_CMS_AUTHORIZE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20.cmsAuthorizeUri";
    private static final String BLUEBUTTON_20_CMS_TOKEN_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20.cmsTokenUri";
    private static final String BLUEBUTTON_20_CMS_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20.cmsBaseUri";
    private static final String BLUEBUTTON_20_REST_CLIENT_ID_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.clientId";
    private static final String BLUEBUTTON_20_REST_CLIENT_SECRET_PROP_KEY = "linuxforhealth.connect.endpoint.bluebutton_20_rest.clientSecret";

    private static final String DATA_STORE_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.datastore.baseUri";
    private static final String DATA_STORE_OPTIONS_PROP_KEY = "linuxforhealth.connect.endpoint.datastore.options";

    private static final String MESSAGING_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.messaging.baseUri";
    
    private static final String ACD_REST_BASE_URI_PROP_KEY = "linuxforhealth.connect.endpoint.acd_rest.baseUri";
    private static final String ACD_REST_FLOW_PROP_KEY = "linuxforhealth.connect.endpoint.acd_rest.flow";

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
     * Builds the FHIR R4 REST URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.fhir_r4_rest.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.fhir_r4_rest.options</li>
     * </ul>
     * @return the FHIR R4 REST URI
     */
    public String getFhirR4RestUri() {
        return appProperties.getProperty(FHIR_R4_REST_BASE_URI_PROP_KEY);
    }

    /**
     * Builds the external FHIR R4 REST URI for a resource using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.external_fhir_r4_rest.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.external_fhir_r4_rest.options</li>
     * </ul>
     * @param resourceType the FHIR resource type
     * @return the FHIR R4 REST URI
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
     * Builds the Blue Button 2.0 REST URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20_rest.baseUri</li>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20_rest.options</li>
     * </ul>
     * @return the Blue Button 2.0 REST URI
     */
    public String getBlueButton20RestUri() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_BASE_URI_PROP_KEY);
    }

    /**
     * Builds the Blue Button 2.0 REST Authorize URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20_rest.authorizeUri</li>
     * </ul>
     * @return the Blue Button 2.0 REST Authorization URI
     */
    public String getBlueButton20RestAuthorizeUri() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_AUTHORIZE_URI_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 REST Callback URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20_rest.callbackUri</li>
     * </ul>
     * @return the Blue Button 2.0 REST Callback URI
     */
    public String getBlueButton20RestCallbackUri() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_CALLBACK_URI_PROP_KEY);
    }
    /**
     * Returns the Blue Button 2.0 REST Token URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20_rest.tokenUri</li>
     * </ul>
     * @return the Blue Button 2.0 REST Token URI
     */
    public String getBlueButton20RestTokenUri() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_TOKEN_URI_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 CMS Authorize URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20.cmsAuthorizeUri</li>
     * </ul>
     * @return the Blue Button 2.0 CMS Authorization URI
     */
    public String getBlueButton20CmsAuthorizeUri() {
        return appProperties.getProperty(BLUEBUTTON_20_CMS_AUTHORIZE_URI_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 CMS Token URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20.cmsTokenUri</li>
     * </ul>
     * @return the Blue Button 2.0 CMS Token URI
     */
    public String getBlueButton20CmsTokenUri() {
        return appProperties.getProperty(BLUEBUTTON_20_CMS_TOKEN_URI_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 CMS Token URI using the following properties:
     * <ul>
     *     <li>linuxforhealth.connect.endpoint.bluebutton_20.cmsTokenUri</li>
     * </ul>
     * @return the Blue Button 2.0 CMS Token URI
     */
    public String getBlueButton20CmsBaseUri() {
        return appProperties.getProperty(BLUEBUTTON_20_CMS_BASE_URI_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 Client Id:
     * <ul>
     *     <li>llinuxforhealth.connect.endpoint.bluebutton_20_rest.clientId</li>
     * </ul>
     * @return the Blue Button 2.0 Client Id
     */
    public String getBlueButton20ClientId() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_CLIENT_ID_PROP_KEY);
    }

    /**
     * Returns the Blue Button 2.0 Client Secret:
     * <ul>
     *     <li>llinuxforhealth.connect.endpoint.bluebutton_20_rest.clientSecret</li>
     * </ul>
     * @return the Blue Button 2.0 Client Secret
     */
    public String getBlueButton20ClientSecret() {
        return appProperties.getProperty(BLUEBUTTON_20_REST_CLIENT_SECRET_PROP_KEY);
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
    
    /**
     * Builds the ACD REST URI
     * @return
     */
    public String getAcdRestUri() {
    	return appProperties.getProperty(ACD_REST_BASE_URI_PROP_KEY);
    }

    /**
     * 
     * @return
     */
    public String getAcdFlow() {
    	return appProperties.getProperty(ACD_REST_FLOW_PROP_KEY);
    }
    
}
