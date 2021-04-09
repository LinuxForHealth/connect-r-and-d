package com.linuxforhealth.connect.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linuxforhealth.connect.processor.MetaDataProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.linuxforhealth.connect.support.CamelContextSupport;

import javax.ws.rs.HttpMethod;

public class KaleidoRouteBuilder extends BaseRouteBuilder {

    
    private final Logger logger = LoggerFactory.getLogger(KaleidoRouteBuilder.class);

    public final static String KALEIDO_ROUTE_ID = "kaleido";
    public final static String KALEIDO_ROUTE_PRODUCER_ID = "kaleido-producer-store-and-notify";

    @Override
    protected String getRoutePropertyNamespace() {return "lfh.connect.kaleido";}

    
    @Override
    // public void configure() throws Exception {
    protected void buildRoute(String routePropertyNamespace) {
        CamelContextSupport ctxSupport = new CamelContextSupport(getContext());
        String kaleidoUri = ctxSupport.getProperty("lfh.connect.kaleido.uri");

        from(kaleidoUri)
            .setHeader(Exchange.HTTP_METHOD,simple("GET"))
            .routeId(KALEIDO_ROUTE_ID)
            .log("Routing to REST")
            .to("https://jsonplaceholder.typicode.com/todos/1?bridgeEndpoint=true&throwExceptionOnFailure=false")
            .process(new MetaDataProcessor(routePropertyNamespace))
            .id(KALEIDO_ROUTE_PRODUCER_ID)
            .log(LoggingLevel.DEBUG, logger, "Response code: ${header.CamelHttpResponseCode}");
    }

}