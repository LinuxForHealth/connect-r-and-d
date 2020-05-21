package com.redhat.idaas.connect.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a Camel Java DSL Route.
 * A Camel Java DSL Route has a consumer and one or more producers.
 */
final class CamelRoute {

    private String routeId;

    private CamelEndpoint consumer = new CamelEndpoint();

    private List<CamelEndpoint> producers = new ArrayList<>();

    void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    String getRouteId() {
        return routeId;
    }

    CamelEndpoint getConsumer() {
        return consumer;
    }

    void setConsumer(CamelEndpoint consumer) {
        this.consumer = consumer;
    }

    List<CamelEndpoint> getProducers() {
        return producers;
    }

    void addProducer(CamelEndpoint producer) {
        producers.add(producer);
    }

    /**
     * Determines if this CamelRoute instance is equal to another object.
     *
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

        CamelRoute otherRoute = (CamelRoute) obj;
        return getRouteId().equals(otherRoute.getRouteId());
    }

    /**
     * @return the hash code for this instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(getRouteId());
    }

    /**
     * @return the Java DSL string representation for this route
     */
    @Override
    public String toString() {
        StringBuilder route = new StringBuilder();
        route.append("from(")
                .append(consumer.toString())
                .append(")\n")
                .append(".routeId(")
                .append(getRouteId())
                .append(")\n");

        if (producers.size() == 1) {
            route.append(".to(")
                    .append(producers.get(0).toString())
                    .append(")\n");
        } else if (producers.size() > 1) {
            route.append(".multicast()\n");
            route.append(".to(");
            for (CamelEndpoint producer : producers) {
                route.append(producer.toString())
                        .append(",");
            }
            route.setLength(route.length() - 1);
            route.append(")\n");
        }
        return route.toString();
    }
}
