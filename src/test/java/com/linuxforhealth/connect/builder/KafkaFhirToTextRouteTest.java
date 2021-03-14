package com.linuxforhealth.connect.builder;

import com.jayway.jsonpath.JsonPath;
import com.linuxforhealth.connect.support.TestUtils;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

public class KafkaFhirToTextRouteTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new KafkaFhirToTextRouteBuilder();
    }

    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpoint(
                KafkaFhirToTextRouteBuilder.ROUTE_ID_LFH_FHIR_MSG,
                NlpRouteBuilder.NLP_ROUTE_URI, "mock:result");

        super.configureContext();
        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Test DocumentReference attachment (2) text extraction
     * @throws Exception
     */
    @Test
    void testRouteDocumentReference() throws Exception {

        String msg = context
                .getTypeConverter()
                .convertTo(String.class,
                        TestUtils.getMessage(
                                "lfh",
                                "lfh-msg-fhir-r4-docref-inline-attach.json"));

        String lfhMsgFhirResourceEnc = JsonPath.parse(msg).read("$.data", String.class);
        String lfhMsgFhirResourceDec = new String(Base64.getDecoder().decode(lfhMsgFhirResourceEnc));

        String fhirResAttach0Enc = JsonPath.parse(lfhMsgFhirResourceDec).read("$.content[0].attachment.data", String.class);
        String fhirResAttach0Dec = new String(Base64.getDecoder().decode(fhirResAttach0Enc));

        String fhirResAttach1Enc = JsonPath.parse(lfhMsgFhirResourceDec).read("$.content[1].attachment.data", String.class);
        String fhirResAttach1Dec = new String(Base64.getDecoder().decode(fhirResAttach1Enc));

        mockResult.expectedMessageCount(2);
        mockResult.expectedBodiesReceived(fhirResAttach0Dec, fhirResAttach1Dec);

        fluentTemplate.to(KafkaFhirToTextRouteBuilder.ROUTE_URI_FHIR_LFH_MSG)
                .withBody(msg)
                .send();

        mockResult.assertIsSatisfied();
    }

    /**
     * Test DiagnosticReport attachment (1) text extraction
     * @throws Exception
     */
    @Test
    void testRouteDiagnosticReport() throws Exception {

        String msg = context
                .getTypeConverter()
                .convertTo(String.class,
                        TestUtils.getMessage(
                                "lfh",
                                "lfh-msg-fhir-r4-diagrpt-inline-attach.json"));

        String lfhMsgFhirResourceEnc = JsonPath.parse(msg).read("$.data", String.class);
        String lfhMsgFhirResourceDec = new String(Base64.getDecoder().decode(lfhMsgFhirResourceEnc));

        String fhirResAttach0Enc = JsonPath.parse(lfhMsgFhirResourceDec).read("$.presentedForm[0].data", String.class);
        String fhirResAttach0Dec = new String(Base64.getDecoder().decode(fhirResAttach0Enc));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(fhirResAttach0Dec);

        fluentTemplate.to(KafkaFhirToTextRouteBuilder.ROUTE_URI_FHIR_LFH_MSG)
                .withBody(msg)
                .send();

        mockResult.assertIsSatisfied();

    }

}