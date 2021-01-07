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

    @Test
    void testRoute() throws Exception {

        String msg = context
                .getTypeConverter()
                .convertTo(String.class,
                        TestUtils.getMessage(
                                "lfh",
                                "lfh-msg-fhir-r4-docref-inline-attach.json"));

        String lfhMsgFhirResourceEnc = JsonPath.parse(msg).read("$.data", String.class);
        String lfhMsgFhirResourceDec = new String(Base64.getDecoder().decode(lfhMsgFhirResourceEnc));

        String fhirResAttachEnc = JsonPath.parse(lfhMsgFhirResourceDec).read("$.content[0].attachment.data", String.class);
        String fhirResAttachDec = new String(Base64.getDecoder().decode(fhirResAttachEnc));

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(fhirResAttachDec);

        fluentTemplate.to(KafkaFhirToTextRouteBuilder.ROUTE_URI_FHIR_LFH_MSG)
                .withBody(msg)
                .send();

        mockResult.assertIsSatisfied();
    }

}