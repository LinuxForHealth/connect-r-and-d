package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.support.FhirAttachmentText;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import javax.ws.rs.core.MediaType;

public class KafkaFhirToTextRouteBuilder extends RouteBuilder {

    @PropertyInject("lfh.connect.nlp.enable")
    private static boolean enableRoute;
    public final static String PROP_RESOURCE_TYPE = "resourceType";

    // Route URIs
    public final static String ROUTE_URI_FHIR_LFH_MSG = "direct:lfh-fhir-msg";
    public final static String ROUTE_URI_FHIR_RESOURCE = "direct:fhir-resource";
    public final static String ROUTE_URI_FHIR_TEXT_DIV = "direct:text-div";
    public final static String ROUTE_URI_FHIR_ATTACH = "direct:attachment";

    // Route IDs
    public final static String ROUTE_ID = "kafka-fhir-to-text";
    public final static String ROUTE_ID_LFH_FHIR_MSG = "lfh-fhir-msg";
    public final static String ROUTE_ID_GET_FHIR = "fhir-process";
    public final static String ROUTE_ID_FHIR_TEXT_DIV = "fhir-text-div";
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

            // Send to div narrative and attachment process in parallel
            .multicast()
                .parallelProcessing()
                .to(ROUTE_URI_FHIR_TEXT_DIV, ROUTE_URI_FHIR_ATTACH)
            ;


        //
        // Extract text from fhir resource attachments
        // INPUT:  FHIR R4 Resource
        // OUTPUT: Plain text (nlp-ready)
        //
        from(ROUTE_URI_FHIR_ATTACH)
            .routeId(ROUTE_ID_FHIR_ATTACH)
            .unmarshal().fhirJson("R4")

            // Split exchange per attachment
            .split().method(FhirAttachmentText.class, "splitAttachments")

            // Run all non plain text messages through Tika
            .filter(header("contentType").not().contains(MediaType.TEXT_PLAIN))
                .to("tika:parse?tikaParseOutputFormat=text")
            .end()

            .to(NlpRouteBuilder.NLP_ROUTE_URI)
            ;


        //
        // Extract text from FHIR R4 resource Narrative (text.div)
        // INPUT:  FHIR R4 Resource, will check for and extract text.div elements
        // OUTPUT: Plain text (nlp-ready)
        //
        from(ROUTE_URI_FHIR_TEXT_DIV)
            .routeId(ROUTE_ID_FHIR_TEXT_DIV)
            .convertBodyTo(String.class)
            .setProperty("resourceTypeElement", constant("narrative"))

            .choice()
                .when().jsonpath("text.div", true)

                    // << CAMEL-15769 Jira issue opened
                    .split(jsonpath("text.div").tokenize("@@@"))

                    // extract text from xhtml tags (e.g. <div>)
                    .to("tika:parse?tikaParseOutputFormat=text")
                    .to(NlpRouteBuilder.NLP_ROUTE_URI)

                .endChoice()
            .end()
            ;

    }

}
