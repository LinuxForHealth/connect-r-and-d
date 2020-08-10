/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.Properties;

/**
 * Provides base configuration and convenience methods for Linux for Health Route Builder tests.
 * Features includes:
 * <ul>
 *     <li>Loading application.properties from the classpath</li>
 *     <li>{@link BeforeEach} method for the {@link org.apache.camel.CamelContext and {@link ProducerTemplate}}</li>
 *     <li>{@link AfterEach} method for the {@link org.apache.camel.CamelContext}</li>
 *     <li>Convenience method for applying "route advice" with {@link AdviceWithRouteBuilder}</li>
 * </ul>
 *
 * {@link AdviceWithRouteBuilder} is used to  redirect a route's producer uri(s) to a MockEndpoint.
 *
 * Test case implementations will typically override {@link RouteTestSupport#configureContext()} to apply route
 * advice and configure a mock endpoint. Note that advice is applied prior to executing the base implementation, which
 * starts the {@link org.apache.camel.CamelContext}.
 * <code>
 *  @Override
 *  protected void configureContext() {
 *      applyAdvice("myRouteId", "producerUri", "mock:result");
 *      super.configureContext();
 *      mockResult = MockEndpoint.resolve(context, "mock:result");
 *  }
 * </code>
 *
 */
abstract class RouteTestSupport extends CamelTestSupport {

    protected ProducerTemplate producerTemplate;

    /**
     * Loads {@link Properties} into the {@link org.apache.camel.CamelContext}
     * @return {@link Properties}
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = null;
        try {
            props = TestUtils.loadProperties("application.properties");
        } catch (IOException ex) {
            Assertions.fail(ex);
        }
        return props;
    }

    /**
     * @return true to signal to {@link CamelTestSupport} that route advice is applied.
     * When {@link CamelTestSupport#isUseAdviceWith()} is true, the {@link org.apache.camel.CamelContext} is not started
     * by {@link CamelTestSupport}, requiring the test case to manage the Camel Context.
     */
    @Override
    public boolean isUseAdviceWith() { return true;}

    /**
     * Applies a {@link AdviceWithRouteBuilder} to a route, redirecting the specified producer to a mock endpoint.
     * {@link AdviceWithRouteBuilder} advises that a route is only "adviced" once. "Advicing" a route more than once
     * may lead to unexpected results.
     *
     * @param routeId The route id to update.
     * @param producerUri The producer uri to intercept.
     * @param mockUri The mock uri which replaces the producer uri.
     * @throws Exception If an error occurs during processing.
     */
    protected void applyAdvice(String routeId, String producerUri, String mockUri) throws Exception  {

        RouteDefinition routeDefinition = context.getRouteDefinition(routeId);
        AdviceWithRouteBuilder advice = new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToUri(producerUri).replace().to(mockUri);
            }
        };

        context.adviceWith(routeDefinition, advice);
    }

    /**
     * Configures the {@link org.apache.camel.CamelContext} prior to test case execution.
     */
    @BeforeEach
    protected void configureContext() throws Exception  {
        context.start();

        producerTemplate = new DefaultProducerTemplate(context);
        producerTemplate.start();
    }

    @AfterEach
    protected void stopContext() {
        context.stop();
    }
}
