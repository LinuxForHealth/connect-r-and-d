package com.redhat.idaas.connect.configuration;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Objects;

/**
 * Models a Camel Java DSL endpoint.
 * A Java DSL endpoint may be a consumer or producer within a Camel Route.
 * The Java DSL endpoint format is a URI structured as [scheme]://[contextPath]?[options]
 * Options are separated by "&"
 */
final class CamelEndpoint {

    private String scheme;

    private String contextPath;

    private final Map<String, String> options = new HashMap<>();

    String getScheme() {
        return scheme;
    }

    void setScheme(String scheme) {
        this.scheme = scheme;
    }

    String getContextPath() {
        return contextPath;
    }

    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Adds an endpoint option
     * @param optionName The option name
     * @param optionValue The option value
     */
    void addOption(String optionName, String optionValue) {
        options.put(optionName, optionValue);
    }

    /**
     * Determines if this CamelEndpoint instance is equal to another object.
     * @param obj The object to compare
     * @return true if the objects are equal, otherwise return false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CamelEndpoint otherEndpoint = (CamelEndpoint) obj;

        return getScheme().equals(otherEndpoint.getScheme()) &&
                getContextPath().equals(otherEndpoint.getContextPath());
    }

    /**
     * @return the hash code for this instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(getScheme(), getContextPath());
    }

    /**
     * @return the string representation for this endpoint
     */
    @Override
    public String toString() {
        StringBuilder endpoint = new StringBuilder();
        String scheme = getScheme() == null ? "" : getScheme();
        String uri = getContextPath() == null ? "" : getContextPath();

        if (!scheme.endsWith(":")) {
            scheme += ":";
        }

        endpoint.append(scheme);

        if (!uri.startsWith("//")) {
            uri = "//" + uri;
        }

        endpoint.append(uri);

        if (options.size() > 0) {
            endpoint.append("?");

            for (Entry<String, String> optionEntry : options.entrySet()) {
                endpoint.append(optionEntry.getKey());
                endpoint.append("=");
                endpoint.append(optionEntry.getValue());
                endpoint.append("&");
            }
            endpoint.setLength(endpoint.length() - 1);
        }
        return endpoint.toString();
    }
}
