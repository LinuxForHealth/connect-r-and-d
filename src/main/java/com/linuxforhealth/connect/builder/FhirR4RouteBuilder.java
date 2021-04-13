/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import com.linuxforhealth.connect.support.CamelContextSupport;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;


import java.util.Base64;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RouteBuilder extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(FhirR4RouteBuilder.class);

    public final static String ROUTE_ID = "fhir-r4";
    public final static String ROUTE_PRODUCER_ID = "fhir-r4-producer-store-and-notify";
    public final static String EXTERNAL_FHIR_ROUTE_URI = "direct:toExternalFhirServers";
    public final static String EXTERNAL_FHIR_ROUTE_ID = "external-fhir-servers";
    public final static String EXTERNAL_FHIR_PRODUCER_ID = "lfh-external-fhir-producer";

    @Override
    protected String getRoutePropertyNamespace() {
        return "lfh.connect.fhir-r4";
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String fhirUri = ctxSupport.getProperty("lfh.connect.fhir-r4.uri");
        rest(fhirUri)
            .post("/{resource}")
            .route()
            .routeId(ROUTE_ID)
            .unmarshal().fhirJson("R4")
            .marshal().fhirJson("R4")
            .process(new MetaDataProcessor(routePropertyNamespace))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)
            .id(ROUTE_PRODUCER_ID)
            .to(EXTERNAL_FHIR_ROUTE_URI);

        /*
         * Optionally send data to one or more external FHIR servers when sending FHIR resources to LinuxForHealth.
         *
         * Set the lfh.connect.fhir-r4.externalserver property to a fhir server path to
         * enable this feature.  Examples:
         *     lfh.connect.fhir-r4.externalserver=http://localhost:9081/fhir-server/api/v4
         *     lfh.connect.fhir-r4.externalserver=https://fhiruser:change-password@192.168.1.205:9443/fhir-server/api/v4
         * If lfh.connect.fhir-r4.externalserver does not exist or is not defined, no exception will be logged.
         *
         * If SSL is needed to connect to the FHIR server:
         * 1. Use 'https' in your FHIR server URL.
         * 2. Set lfh.connect.fhir-r4.verifycerts=false if using self signed certs.
         * 3. Add the cert for your FHIR server to the LinuxForHealth truststore via the following steps:
         *    - Obtain the FHIR server certificate:
         *        cd connect/container-support/certs
         *        echo | openssl s_client -connect 192.168.1.205:9443 -showcerts 2>/dev/null | openssl x509 > fhir-server.cer
         *    - Import the FHIR server certificate into the LinuxForHealth truststore:
         *        keytool -keystore lfhtruststore.jks -alias fhir-server -import -file ./fhir-server.cer \
         *            -noprompt -storetype pkcs12 -storepass change-password
         *        cp lfhtruststore.jks ../../src/main/resources
         *    - If using the LinuxForHealth connect docker image, you'll need to rebuild and push the image:
         *        docker buildx build --pull --push --platform linux/amd64,linux/s390x,linux/arm64 \
         *            -t docker.io/<your_repo>/connect:<your_tag> .
         *      then modify LFH_CONNECT_IMAGE in connect/container-support/compose/.env to use your new image.
         */ 
        from(EXTERNAL_FHIR_ROUTE_URI)
        .routeId(EXTERNAL_FHIR_ROUTE_ID)
        .choice()
			.when(simple("${properties:lfh.connect.fhir-r4.externalserver:doesnotexist} == 'doesnotexist'"))
				.stop()
            .when(simple("${properties:lfh.connect.fhir-r4.externalserver} == ''"))
				.stop()
        .end()
        .process(exchange -> {
            // Save off the existing result in a property
            exchange.setProperty("result", exchange.getIn().getBody(String.class));

            // Decode the data and set as message body
            JSONObject msg = new JSONObject(exchange.getIn().getBody(String.class));
            byte[] body = Base64.getDecoder().decode(msg.getString("data"));
            exchange.getIn().setBody(body);

            // Set headers for the outbound call
            exchange.getIn().removeHeaders("Camel*");
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
            exchange.getIn().setHeader("Prefer", "return=OperationOutcome");

            // Set SSL params for the outbound call
            String fhirURI = simple("${properties:lfh.connect.fhir-r4.externalserver}").evaluate(exchange, String.class);
            Boolean verifyCerts = simple("${properties:lfh.connect.fhir-r4.verifycerts}").evaluate(exchange, Boolean.class);
            String fhirParams = "?bridgeEndpoint=true&throwExceptionOnFailure=false";
            if (fhirURI.contains("https")) {
                fhirParams += "&sslContextParameters=#sslContextParameters";
                if (!verifyCerts.booleanValue()) fhirParams += "&x509HostnameVerifier=#noopHostnameVerifier";
            }
            exchange.setProperty("fhir-params", fhirParams);
        })
        .log(LoggingLevel.DEBUG, logger, "Sending to external fhir server: ${properties:lfh.connect.fhir-r4.externalserver}/${header[resource]}${exchangeProperty[fhir-params]}")
        .toD("${properties:lfh.connect.fhir-r4.externalserver}/${header[resource]}${exchangeProperty[fhir-params]}")
        .id(EXTERNAL_FHIR_PRODUCER_ID);
    }
}
