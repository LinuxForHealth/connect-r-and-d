package com.linuxforhealth.connect.builder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.PredicateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaFhirToTextBuilder extends RouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(KafkaFhirToTextBuilder.class);
    final static String PROP_PATIENT_ID = "patientId";
    final static String PROP_ID = "id";
    final static String PROP_RESOURCE_TYPE = "resourceType";

    @Override
    public void configure() throws Exception {

        getContext().setStreamCaching(true);

        //
        // Consume topic messages from stream
        // INPUT:  LinuxForHealth Message Envelope
        // OUTPUT: FHIR R4 Resource
        //
        from("kafka:FHIR-R4_DOCUMENTREFERENCE,FHIR-R4_DIAGNOSTICREPORT,FHIR-R4_BUNDLE?brokers={{lfh.connect.datastore.brokers}}")
            .log(LoggingLevel.DEBUG, logger, "[kafka-input]:\n ${body.substring(0,200)}")

            .setProperty("dataFormat").jsonpath("meta.dataFormat")

            .choice()

                .when(exchangeProperty("dataFormat").isEqualTo("FHIR-R4"))
                    .split().jsonpath("data", true)
                    .unmarshal().base64().convertBodyTo(String.class)

                    .setProperty(PROP_RESOURCE_TYPE).jsonpath("resourceType", true)

                    .choice()

                        .when(exchangeProperty(PROP_RESOURCE_TYPE).isEqualTo("Bundle"))
                        .split().jsonpath("entry[*].resource")
                        .marshal().json()
                        .to("direct:fhir-resource")
                        .endChoice()

                    .end()

                    .to("direct:fhir-resource")

                .endChoice()

            .end()
        ;


        //
        // Process individual FHIR R4 resources
        // INPUT:  FHIR R4 Resource
        // OUTPUT: FHIR R4 Resource (routed appropriately)
        //
        from("direct:fhir-resource")
            .convertBodyTo(String.class)
            .log(LoggingLevel.DEBUG, logger, "[fhir-resource] INPUT:\n${body.substring(0,200)}")

            .setProperty(PROP_RESOURCE_TYPE).jsonpath("resourceType", true)
            .setProperty(PROP_PATIENT_ID).jsonpath("subject.reference", true)
            .setProperty(PROP_ID).jsonpath("id", true)

            .choice()

                .when(exchangeProperty(PROP_RESOURCE_TYPE).isEqualTo("DocumentReference"))
                    .multicast()
                    .parallelProcessing()
                    .to("direct:text-div", "direct:documentreference-attachment")
                .endChoice()

                .when(exchangeProperty(PROP_RESOURCE_TYPE).isEqualTo("DiagnosticReport"))
                    .multicast()
                    .parallelProcessing()
                    .to("direct:text-div", "direct:diagnosticreport-attachment")
                .endChoice()

                .otherwise()
                    .to("direct:text-div")
                .endChoice()

            .end()
        ;


        //
        // Extract text from FHIR R4 resource Narrative (text.div)
        // INPUT:  FHIR R4 Resource, will check for and extract text.div elements
        // OUTPUT: Unstructured text (nlp-ready)
        //
        from("direct:text-div")
            .log(LoggingLevel.DEBUG, logger, "[text-div] INPUT:\n${body.substring(0,200)}")
            .setProperty("resourceTypeElement", constant("narrative"))

            .choice()
                .when().jsonpath("text.div", true)

                    // << CAMEL-15769 Jira issue opened
                    .split(jsonpath("text.div").tokenize("@@@"))

                    .log(LoggingLevel.DEBUG, logger, "[text-div] before tika:\n${body}")
                    // extract text from xhtml tags (e.g. <div>)
                    .to("tika:parse?tikaParseOutputFormat=text")
                    .log(LoggingLevel.DEBUG, logger, "[text-div] after tika:\n${body}")

                    .to(NlpRouteBuilder.NLP_ROUTE_URI)

                .endChoice()
            .end()
        ;


        //
        // Extract DiagnosticReport attachment
        // INPUT:  FHIR R4 DiagnosticReport Resource
        // OUTPUT: Attachment
        //
        from("direct:diagnosticreport-attachment")
            .choice()
                .when().jsonpath("presentedForm", true)
                    .split().jsonpath("presentedForm", true)
                    .to("direct:attachment")
                .endChoice()
            .end()
        ;


        //
        // Extract DocumentReference attachments
        // INPUT:  FHIR R4 DocumentReference Resource
        // OUTPUT: Attachment
        //
        from("direct:documentreference-attachment")
            .choice()
                .when().jsonpath("content[*].attachment", true)
                    .split().jsonpath("content[*].attachment", true)
                    .to("direct:attachment")
                .endChoice()
            .end()
        ;


        //
        // Extract text from FHIR R4 resource attachments
        // INPUT:  Attachment
        // OUTPUT: Unstructured text (nlp-ready)
        //
        from("direct:attachment")

            .setProperty("contentType").jsonpath("contentType", true)
            .setProperty("resourceTypeElement", constant("attachment"))
            .split().jsonpath("data", true)
            .unmarshal().base64()

            .choice()

                .when(PredicateBuilder.or(
                    exchangeProperty("contentType").contains("application/pdf"),
                    exchangeProperty("contentType").contains("text/html")))

                    .log(LoggingLevel.DEBUG, logger, "[attachment] before tika:\n${body}")
                    // Convert PDF message to Text
                    .to("tika:parse?tikaParseOutputFormat=text")
                    .log(LoggingLevel.DEBUG, logger, "[attachment] after tika:\n${body}")
                    .to(NlpRouteBuilder.NLP_ROUTE_URI)
                .endChoice()

                .when(exchangeProperty("contentType").contains("text/plain"))
                    .to(NlpRouteBuilder.NLP_ROUTE_URI)
                .endChoice()

            .end()
        ;

    }
}
