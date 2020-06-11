package com.redhat.idaas.connect.builder;

import com.redhat.idaas.connect.configuration.EndpointUriBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import com.redhat.idaas.connect.processor.KafkaToNATS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a HL7 V2 MLLP processing route
 */
public class Hl7v2MllpRouteBuilder extends IdaasRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(Hl7v2MllpRouteBuilder.class);

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
                .setHeader(KafkaConstants.KEY, constant("Camel"))
                .doTry()
                    .toD(producerUri)
                    .process(kafkaToNATS)
                    .to(messagingUri)
                .doCatch(Exception.class)
                   .setBody(exceptionMessage())
                   .log(LoggingLevel.ERROR, logger, "${body}")
                .end();
    }
}
