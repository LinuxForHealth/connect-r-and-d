/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import com.linuxforhealth.connect.processor.FormatMessageProcessor;
import com.linuxforhealth.connect.processor.FormatNotificationProcessor;
import com.linuxforhealth.connect.processor.FormatErrorProcessor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the Linux for Health direct routes
 */
public class DirectRouteBuilder extends LinuxForHealthRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(DirectRouteBuilder.class);

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        String messagingUri = uriBuilder.getMessagingUri();

        Processor formatMessage = new FormatMessageProcessor();
        Processor formatNotification = new FormatNotificationProcessor();
        Processor formatError = new FormatErrorProcessor();

        from("direct:storeandnotify")
                .doTry()
                    .setHeader(KafkaConstants.KEY, constant("Camel"))
                    .process(formatMessage)
                    .toD("${exchangeProperty[dataStoreUrl]}")
                    .to("direct:notify")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();

        from("direct:store")
                .doTry()
                    .process(formatMessage)
                    .toD("${headers[dataStoreUrl]}")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();

        from("direct:notify")
                .doTry()
                    .process(formatNotification)
                    .to(messagingUri)
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to("direct:error")
                .end();

        from("direct:error")
                .doTry()
                    .process(formatError)
                    .log(LoggingLevel.ERROR, logger, "${exchangeProperty[errorMessage]}")
                    .to(messagingUri)
                .doCatch(Exception.class)
                    .log(LoggingLevel.ERROR, logger, exceptionMessage().toString())
                .end();
    }
}
