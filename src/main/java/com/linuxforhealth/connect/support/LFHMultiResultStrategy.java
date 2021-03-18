/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;

/**
 * Aggregation Strategy used in routes which split inbound messages and require an aggregated LFH Message response.
 */
public class LFHMultiResultStrategy implements AggregationStrategy {

    /**
     * Aggregates the "old"/previous {@link Exchange} with the "new"/current {@link Exchange} creating a combined result.
     * @param oldExchange The old/previous exchange
     * @param newExchange The new/current exchange
     * @return The "old"/previous {@link Exchange}
     */
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        if (oldExchange == null) {
            String body = new String(newExchange.getIn().getBody(byte[].class));
            if (body.equals("")) {
                body = "{}";
            }
            JSONObject newResult = new JSONObject(body);
            JSONArray results = new JSONArray(Collections.singletonList(newResult));
            newExchange.getIn().setBody(results.toString(), String.class);
            return newExchange;
        }

        JSONArray aggregatedResults = new JSONArray(oldExchange.getIn().getBody(String.class));
        String body = new String(newExchange.getIn().getBody(byte[].class));
        if (body.equals("")) {
            body = "{}";
        }
        JSONObject newResult = new JSONObject(body);
        aggregatedResults.put(newResult);
        oldExchange.getIn().setBody(aggregatedResults.toString(), String.class);
        return oldExchange;
    }
}
