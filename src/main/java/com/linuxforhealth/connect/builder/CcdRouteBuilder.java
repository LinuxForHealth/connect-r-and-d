package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.MetaDataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a CCD Processing Route.
 * The CCD Route utilizes the C-CDA 2.x format defined within the HL7 v3 specification.
 * Schema validation is optional using the lfh.connect.ccd.validate property.
 */
public class CcdRouteBuilder extends BaseRouteBuilder {

    public final static String ROUTE_ID = "ccd";
    private final static String ROUTE_PROPERTY_NAMESPACE = "lfh.connect.ccd";

    @Override
    protected String getRoutePropertyNamespace() {
        return ROUTE_PROPERTY_NAMESPACE;
    }

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        from("{{lfh.connect.ccd.uri}}")
        .routeId(ROUTE_ID)
        .marshal().mimeMultipart()
        .choice()
            .when(simple("${properties:lfh.connect.ccd.validate:false} == 'true'"))
            .to("validator:CDASchemas/cda/Schemas/CDA.xsd")
            .endChoice()
        .end()
        .process(new MetaDataProcessor(routePropertyNamespace))
        .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI);
    }
}
