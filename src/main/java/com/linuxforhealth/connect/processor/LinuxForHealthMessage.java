/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.util.Arrays;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Extend JSONObject to override toString to print attributes in a specific order.
 */
public class LinuxForHealthMessage extends JSONObject {
    private JSONObject meta;

    public LinuxForHealthMessage() {}

    // Set up JSON structure and common fields
    public LinuxForHealthMessage(Exchange exchange) {
        meta = new JSONObject();
        meta.put("routeId", exchange.getFromRouteId());
        meta.put("uuid", exchange.getProperty("uuid", String.class));
        meta.put("routeUrl", exchange.getProperty("routeUrl", String.class));
        meta.put("dataFormat", exchange.getProperty("dataFormat", String.class));
        meta.put("timestamp", exchange.getProperty("timestamp", String.class));
        meta.put("dataStoreUri", exchange.getProperty("dataStoreUri", String.class));
        this.put("meta", meta);
    }

    // Set error fields
    public void setError(String errorMsg) {
        meta.put("status", "error");
        this.put("data", errorMsg);
    }

    // Set fields for successful data storage
    public void setDataStoreResult(List<RecordMetadata> metaRecords) {
        JSONArray kafkaMeta  = new JSONArray();

        if (metaRecords != null) {
            for (RecordMetadata m: metaRecords) {
                kafkaMeta.put(m);
            }
            meta.put("status", "success");
        } else {
            meta.put("status", "error");
        }

        meta.put("dataRecordLocation", kafkaMeta);
    }

    // Set the data field to the data to be stored
    public void setData(Object data) {
        this.put("data", data);
    }

    /**
     * Override to support ordered fields and rendering of polymorphic data.
     */
    @Override
    public String toString() {
        String result = "{\"meta\":{"+
            getString(meta, "routeId")+","+
            getString(meta, "uuid")+","+
            getString(meta, "routeUrl")+","+
            getString(meta, "dataFormat")+","+
            getObject(meta, "timestamp")+","+
            getString(meta, "dataStoreUri");

        if (meta.has("status")) result += ","+getString(meta, "status");
        if (meta.has("dataRecordLocation")) result += ","+getObject(meta, "dataRecordLocation");
        result += "}";
        if (this.has("data")) result += ","+getObjectString(this, "data");
        result += "}";

        return result;
    }

    private String getString(JSONObject obj, String name) {
        return "\""+name+"\":\""+obj.getString(name)+"\"";
    }

    private String getJsonString(JSONObject obj, String name) {
    	 return "\""+name+"\":" + obj.getString(name);
    }

    private String getObject(JSONObject obj, String name) {
        return "\""+name+"\":"+obj.get(name).toString();
    }

    // Extend to support different object types as needed
    private String getObjectString(JSONObject obj, String name) {
        Object dataObj = (Object) obj.get(name);
        String result;

        if (dataObj instanceof byte[]) {
            result = "\""+name+"\":"+Arrays.toString((byte[]) dataObj);
        } else if (dataObj instanceof String) {
          // If data value is json, do not enclose the brackets in quotation marks (invalid json)
          if (isJson(this.get("data").toString())) result = getJsonString(obj, name);
          else result = getString(obj, name);
        } else {
            result = getObject(obj, name);
        }

        return result;
    }

    // Determine whether/not a string is json
    private boolean isJson(String str) {
    	try {
    	    new JSONObject(str);
    	    return true;
    	} catch (JSONException e) { return false; }
    }
    
}
