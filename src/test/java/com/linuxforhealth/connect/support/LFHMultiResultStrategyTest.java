/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LFHMultiResultStrategy} used to merge LFH messaging results into a single response
 */
public class LFHMultiResultStrategyTest {

    private LFHMultiResultStrategy strategy = new LFHMultiResultStrategy();

    private Exchange oldExchange;
    private Exchange newExchange;

    @BeforeEach
    void beforeEach() {
        CamelContext ctx = new DefaultCamelContext();
        oldExchange = new DefaultExchange(ctx);
        newExchange = new DefaultExchange(ctx);
    }

    /**
     * Tests the "first" run of the aggregate strategy where the new exchange is present and the old exchange is null.
     */
    @Test
    void testAggregateOldExchangeNull() {
        String expectedJson = "[{\"foo\":\"bar\"}]";

        oldExchange = null;
        newExchange.getIn().setBody("{\"foo\":\"bar\"}", String.class);

        Exchange actualExchange = strategy.aggregate(oldExchange, newExchange);
        String actualJson = actualExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedJson, actualJson);
    }

    /**
     * Tests the aggregate strategy with an old and new exchange.
     */
    @Test
    void testAggregate() {
        String expectedJson = "[{\"foo\":\"bar\"},{\"moo\":\"boo\"}]";

        oldExchange.getIn().setBody("[{\"foo\":\"bar\"}]", String.class);
        newExchange.getIn().setBody("{\"moo\":\"boo\"}", String.class);

        Exchange actualExchange = strategy.aggregate(oldExchange, newExchange);
        String actualJson = actualExchange.getIn().getBody(String.class);
        Assertions.assertEquals(expectedJson, actualJson);
    }
}
