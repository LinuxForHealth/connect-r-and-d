/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set up request for Blue Button 2.0 authorization code
 */
public class BlueButton20AuthProcessor extends LinuxForHealthProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(BlueButton20AuthProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder(exchange);
        String callbackURL = uriBuilder.getBlueButton20RestCallbackUri();
        String cmsAuthorizeURL = uriBuilder.getBlueButton20CmsAuthorizeUri();
        String clientId = uriBuilder.getBlueButton20ClientId();

        // Set up call to redirect to Blue Button API so the user can authenticate this application
        String authorizeURL = cmsAuthorizeURL+
            "?client_id="+clientId+
            "&redirect_uri="+callbackURL+
            "&response_type=code";
        logger.info("Authorize URL: "+authorizeURL);

        // Determine the current OS so we know the cmd to launch the browser
        String osCmd;
        if (SystemUtils.IS_OS_MAC) {
            osCmd = "open";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            osCmd = "explorer";
        } else {
            // Assume SystemUtils.IS_OS_UNIX
            osCmd = "xdg-open";
        }

        exchange.setProperty("location", "exec:"+osCmd+"?args=RAW("+authorizeURL+")");
    }
}
