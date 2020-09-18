/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregate results from multiple servers, as well as LFH data storage result.
 */
public class ExternalServerAggregationStrategy implements AggregationStrategy {

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            // Get the original result - the data store location
            String result = newExchange.getProperty("result", String.class);
            JSONObject resultObj = new JSONObject(result);

            // Add the location header to the new result
            JSONObject msg = addHeaderToResult(newExchange);

            // Create JSONObject[] and add new result to it
            JSONObject[] resultArray = new JSONObject[1];
            resultArray[0] = msg;

            // Place both results in a new JSON object & set that as the result
            JSONObject metaResult = new JSONObject();
            metaResult.put("DataStoreResult", resultObj);
            metaResult.put("ExternalServerResult", resultArray);
            newExchange.getIn().setBody(metaResult.toString());
            return newExchange;
        }

        // Get the new result and add the location header
        JSONObject msg = addHeaderToResult(newExchange);

        // Get the meta result from the old exchange, add new result and set as body
        JSONObject metaResult = new JSONObject(oldExchange.getIn().getBody(String.class));
        JSONObject newMeta = metaResult.append("ExternalServerResult", msg);
        oldExchange.getIn().setBody(newMeta.toString());
        return oldExchange;
    }

    private JSONObject addHeaderToResult(Exchange exchange) {
        JSONObject msg = new JSONObject();
        JSONObject result = new JSONObject(exchange.getIn().getBody(String.class));
        msg.put("Location", exchange.getIn().getHeader("Location", String.class));
        msg.put("Result", result);
        return msg;
    }
}