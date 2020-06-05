package com.redhat.idaas.connect.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRoute  extends RouteBuilder{
    @Override
    public void configure() {
        String consumerUri = "{{idaas.connect.endpoint.hl7_v2_mllp.baseUri}}"
                .concat("?")
                .concat("{{idaas.connect.endpoint.hl7_v2_mllp.options}}");

        String producerUri = "{{idaas.connect.endpoint.messaging.baseUri}}${headers[CamelHL7MessageType]}"
                .concat("?")
                .concat("{{idaas.connect.endpoint.messaging.options}}");

        from(consumerUri)
                .routeId("hl7-v2-mllp")
                .unmarshal().hl7()
                .log(LoggingLevel.INFO, "${body}")
                .setHeader(KafkaConstants.KEY, constant("Camel"))
                .toD(producerUri);
    }
}
