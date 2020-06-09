package com.redhat.idaas.connect.builder;

import com.redhat.idaas.connect.configuration.EndpointUriBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import com.redhat.idaas.connect.processor.KafkaToNATS;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends IdaasRouteBuilder {

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        String consumerUri = uriBuilder.getHl7V2MllpUri();
        String producerUri = uriBuilder.getDataStoreUri("HL7v2_${headers[CamelHL7MessageType]}");
        String messagingUri = uriBuilder.getMessagingUri();
        Processor kafkaToNATS = new KafkaToNATS();

        from(consumerUri)
                .routeId("hl7-v2-mllp")
                .unmarshal().hl7()
                .log(LoggingLevel.INFO, "${body}")
                .setHeader(KafkaConstants.KEY, constant("Camel"))
                .doTry()
                    .toD(producerUri)
                    .process(kafkaToNATS)
                    .to(messagingUri)
                .doCatch(Exception.class)
                   .setBody(exceptionMessage())
                   .log(LoggingLevel.ERROR, "${body}")
                .end();
    }
}
