/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.ConstantExpression;
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
 *     <li>{@link BeforeEach} method for the {@link org.apache.camel.CamelContext and {@link this#fluentTemplate()}}</li>
 *     <li>{@link AfterEach} method for the {@link org.apache.camel.CamelContext}</li>
 *     <li>Convenience method for mocking endpoints using {@link AdviceWithRouteBuilder}</li>
 * </ul>
 *
 * Test case implementations will typically override {@link RouteTestSupport#configureContext()} to mock an endpoint.
 * Note that mocks/advice are applied prior to executing the base implementation, which starts the
 * {@link org.apache.camel.CamelContext}.
 * <code>
 *  @Override
 *  protected void configureContext() {
 *      mockProducerEndpoint("myRouteId", "producerUri", "mock:result");
 *      super.configureContext();
 *      mockResult = MockEndpoint.resolve(context, "mock:result");
 *  }
 * </code>
 */
abstract class RouteTestSupport extends CamelTestSupport {

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
     * @return true to signal to {@link CamelTestSupport} that route advice is used to mock endpoints.
     * When {@link CamelTestSupport#isUseAdviceWith()} is true, the {@link org.apache.camel.CamelContext} is not started
     * by {@link CamelTestSupport}, requiring the test case to manage the Camel Context.
     */
    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Mocks a producer endpoint by producer id using a {@link AdviceWithRouteBuilder}.
     *
     * @param routeId     The route id to update.
     * @param producerId The producer uri to mock.
     * @param mockUri     The mock uri which replaces the producer uri.
     * @throws Exception If an error occurs during processing.
     */
    protected void mockProducerEndpointById(String routeId, String producerId, String mockUri) throws Exception {

        RouteDefinition routeDefinition = context.getRouteDefinition(routeId);
        AdviceWithRouteBuilder advice = new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(producerId).replace().to(mockUri);
            }
        };

        context.adviceWith(routeDefinition, advice);
    }

    /**
     * Intercepts a producer endpoint using {@link AdviceWithRouteBuilder} to return a sample, or "mock" response.
     * The sample of mock response is loaded from the file system using {@link TestUtils#getMessage(String, String)}
     * Producer endpoints are matched using "id".
     *
     * @param routeId The route id where the producer is located.
     * @param producerId The producer id
     * @param messageDirectory The directory in the test source tree where the message is located.
     * @param messageName The message file name
     */
    protected void setProducerResponse(String routeId,
                                       String producerId,
                                       String messageDirectory,
                                       String messageName) throws Exception {

        String mockedResponse = context
                .getTypeConverter()
                .convertTo(String.class, TestUtils.getMessage(messageDirectory, messageName));

        RouteDefinition routeDefinition = context.getRouteDefinition(routeId);
        AdviceWithRouteBuilder advice = new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(producerId).replace().setBody(new ConstantExpression(mockedResponse));
            }
        };
        context.adviceWith(routeDefinition, advice);
    }

   /**
     * Replaces a consumer with a mock consumer for a route.
     *
     * @param routeId The route id where the producer is located.
     * @param mockRoute The mocked direct route.  Example: "direct:kafka-from"
     */
    protected void mockConsumer(String routeId, String mockRoute) throws Exception {
        // Swap the FROM component in the route with a direct component
        AdviceWithRouteBuilder.adviceWith(context, 
            routeId, 
            routeBuilder -> {
                routeBuilder.replaceFromWith(mockRoute);
        });
    }

    /**
     * Configures the {@link org.apache.camel.CamelContext} prior to test case execution.
     */
    @BeforeEach
    protected void configureContext() throws Exception {
        context.start();
        fluentTemplate.start();
    }

    @AfterEach
    protected void stopContext() {
        fluentTemplate.stop();
        context.stop();
    }
}
