package com.linuxforhealth.connect.builder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.PredicateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaFhirToTextRouteBuilder extends RouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(KafkaFhirToTextRouteBuilder.class);
    @PropertyInject("lfh.connect.nlp.enable")
    private static boolean enableRoute;
    final static String PROP_RESOURCE_TYPE = "resourceType";

    // Route URIs
    public final static String ROUTE_URI_FHIR_LFH_MSG = "direct:lfh-fhir-msg";
    public final static String ROUTE_URI_FHIR_RESOURCE = "direct:fhir-resource";
    public final static String ROUTE_URI_FHIR_TEXT_DIV = "direct:text-div";
    public final static String ROUTE_URI_FHIR_DIAGRPT_ATTACH = "direct:diagnosticreport-attachment";
    public final static String ROUTE_URI_FHIR_DOCREF_ATTACH = "direct:documentreference-attachment";
    public final static String ROUTE_URI_FHIR_ATTACH = "direct:attachment";

    // Route IDs
    public final static String ROUTE_ID = "kafka-fhir-to-text";
    public final static String ROUTE_ID_LFH_FHIR_MSG = "lfh-fhir-msg";
    public final static String ROUTE_ID_GET_FHIR = "fhir-process";
    public final static String ROUTE_ID_FHIR_TEXT_DIV = "fhir-text-div";
    public final static String ROUTE_ID_FHIR_DIAGRPT_ATTACH = "fhir-diagrpt-attach";
    public final static String ROUTE_ID_FHIR_DOCREF_ATTACH = "fhir-docref-attach";
    public final static String ROUTE_ID_FHIR_ATTACH = "fhir-attach";

    @Override
    public void configure() throws Exception {

        getContext().setStreamCaching(true);

        //
        // Consume topic messages from stream
        // Note: breaking out the kafka consumer route from the route that processes
        //       LFH messages retrieved from the stream.
        // INPUT:  LFH Message Envelope
        // OUTPUT: LFH Message Envelope
        //
        from("kafka:{{lfh.connect.nlp.fhir-topics}}?brokers={{lfh.connect.datastore.brokers}}")
            .routeId(ROUTE_ID)
            .autoStartup(enableRoute) // property-based route enablement toggle
            .log(LoggingLevel.DEBUG, logger, "[kafka-input]:\n ${body}")
            .to(ROUTE_URI_FHIR_LFH_MSG)
            ;


        //
        // Consume/process FHIR resource LFH messages
        // INPUT:  LFH Message Envelope
        // OUTPUT: FHIR R4 Resource
        //
        from(ROUTE_URI_FHIR_LFH_MSG)
            .routeId(ROUTE_ID_LFH_FHIR_MSG)
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
                        .to(ROUTE_URI_FHIR_RESOURCE)
                        .endChoice()

                    .end()

                    .to(ROUTE_URI_FHIR_RESOURCE)

                .endChoice()

            .end()
        ;


        //
        // Process individual FHIR R4 resources
        // INPUT:  FHIR R4 Resource
        // OUTPUT: FHIR R4 Resource (routed appropriately)
        //
        from(ROUTE_URI_FHIR_RESOURCE)
            .routeId(ROUTE_ID_GET_FHIR)
            .convertBodyTo(String.class)
            .log(LoggingLevel.DEBUG, logger, "[fhir-resource] INPUT:\n${body}")

            .setProperty(PROP_RESOURCE_TYPE).jsonpath("resourceType", true)

            .choice()

                .when(exchangeProperty(PROP_RESOURCE_TYPE).regex("DocumentReference|DiagnosticReport"))
                    .recipientList(
                            simple(ROUTE_URI_FHIR_TEXT_DIV + ", direct:${exchangeProperty.resourceType.toLowerCase()}-attachment"), ",")
                    .parallelProcessing()
                .endChoice()

                .otherwise()
                    .to(ROUTE_URI_FHIR_TEXT_DIV)
                .endChoice()

            .end()
        ;


        //
        // Extract text from FHIR R4 resource Narrative (text.div)
        // INPUT:  FHIR R4 Resource, will check for and extract text.div elements
        // OUTPUT: Unstructured text (nlp-ready)
        //
        from(ROUTE_URI_FHIR_TEXT_DIV)
            .routeId(ROUTE_ID_FHIR_TEXT_DIV)
            .log(LoggingLevel.DEBUG, logger, "[text-div] INPUT:\n${body}")
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
        from(ROUTE_URI_FHIR_DIAGRPT_ATTACH)
            .routeId(ROUTE_ID_FHIR_DIAGRPT_ATTACH)
            .choice()
                .when().jsonpath("presentedForm", true)
                    .split().jsonpath("presentedForm", true)
                    .to(ROUTE_URI_FHIR_ATTACH)
                .endChoice()
            .end()
        ;


        //
        // Extract DocumentReference attachments
        // INPUT:  FHIR R4 DocumentReference Resource
        // OUTPUT: Attachment
        //
        from(ROUTE_URI_FHIR_DOCREF_ATTACH)
            .routeId(ROUTE_ID_FHIR_DOCREF_ATTACH)
            .choice()
                .when().jsonpath("content[*].attachment", true)
                    .split().jsonpath("content[*].attachment", true)
                    .to(ROUTE_URI_FHIR_ATTACH)
                .endChoice()
            .end()
        ;


        //
        // Extract text from FHIR R4 resource attachments
        // INPUT:  Attachment
        // OUTPUT: Unstructured text (nlp-ready)
        //
        from(ROUTE_URI_FHIR_ATTACH)
            .routeId(ROUTE_ID_FHIR_ATTACH)
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
