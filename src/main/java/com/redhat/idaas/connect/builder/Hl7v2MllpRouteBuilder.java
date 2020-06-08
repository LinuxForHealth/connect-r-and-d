package com.redhat.idaas.connect.builder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends IdaasRouteBuilder {
    @Override
    public void configure() {
        String producerUri = getDataStoreUri("HL7v2_${headers[CamelHL7MessageType]}");

        from(getHl7V2MllpUri())
                .routeId("hl7-v2-mllp")
                .unmarshal().hl7()
                .log(LoggingLevel.INFO, producerUri)
                .log(LoggingLevel.INFO, "${body}")
                .setHeader(KafkaConstants.KEY, constant("Camel"))
                .toD(producerUri);
    }
}
