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

    private String options;

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

    String getOptions() { return options; }

    void setOptions(String options) { this.options = options; }

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
     * @return the URI string representation for this endpoint
     */
    @Override
    public String toString() {
        StringBuilder endpointUri = new StringBuilder();
        String scheme = getScheme() == null ? "" : getScheme();
        String context = getContextPath() == null ? "" : getContextPath();

        endpointUri.append(scheme)
            .append(context);

        if (options != null && options.length()  > 1) {
            endpointUri.append("?");
            endpointUri.append(options);
        }
        return endpointUri.toString();
    }
}
