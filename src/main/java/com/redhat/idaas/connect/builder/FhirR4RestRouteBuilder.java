package com.redhat.idaas.connect.builder;

import com.redhat.idaas.connect.configuration.EndpointUriBuilder;
import org.apache.camel.LoggingLevel;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RestRouteBuilder extends IdaasRouteBuilder {

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        URI fhirBaseUri = URI.create(uriBuilder.getFhirR4RestUri());
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_${headers.resourceType.toUpperCase()}");

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId("fhir-r4-rest")
                .unmarshal().fhirJson("R4")
                .setHeader("resourceType",simple("${body.resourceType.toString()}"))
                .marshal().fhirJson("R4")
                .log(LoggingLevel.INFO, "${body}")
                .toD(kafkaDataStoreUri);
    }
}
