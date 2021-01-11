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

import javax.ws.rs.HttpMethod;

public class NlpRouteBuilder extends BaseRouteBuilder {

    public final static String NLP_ROUTE_URI = "direct:nlp";
    @PropertyInject("lfh.connect.nlp.request-json")
    private static String requestJson;
    @PropertyInject("lfh.connect.nlp.enable")
    private static boolean enableRoute;
    private final Logger logger = LoggerFactory.getLogger(NlpRouteBuilder.class);

    public final static String NLP_ROUTE_ID = "nlp";

    @Override
    protected String getRoutePropertyNamespace() {return "lfh.connect.nlp";}

    @Override
    protected void buildRoute(String routePropertyNamespace) {

        getContext().setStreamCaching(true); // Prevent exchange message body from disappearing after reads

        onException(Exception.class)
            .log(LoggingLevel.DEBUG, logger, "${body}")
        ;

        //
        // Route unstructured data to configured NLP service for analysis
        // INPUT:  Unstructured data
        // OUTPUT: NLP service response wrapped in LFH message
        //
        from(NLP_ROUTE_URI)
            .routeId(NLP_ROUTE_ID)
            .autoStartup(enableRoute)
            .log(LoggingLevel.DEBUG, logger, "Received: ${body}")
            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
            .setHeader(Exchange.CONTENT_TYPE, constant(ContentType.TEXT_PLAIN))
            .convertBodyTo(String.class)

            // Wrap text in json if request-json template property defined
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    logger.debug("wrap request text in json: " + !"".equalsIgnoreCase(requestJson));
                    if (!"".equalsIgnoreCase(requestJson)) {
                        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, ContentType.APPLICATION_JSON);
                        String json = new ObjectMapper().writeValueAsString(exchange.getIn().getBody(String.class));
                        json = requestJson.replace("<REPLACE_TOKEN>", json);
                        try {
                            JSONObject jsonObj = new JSONObject(json);
                            exchange.getIn().setBody(jsonObj.toString());
                        } catch (JSONException e) {
                            logger.error("Error occurred wrapping text in request json", e);
                            throw e;
                        }
                    }
                }
            })

            .to("{{lfh.connect.nlp.uri}}{{lfh.connect.nlp.auth}}")
            .log(LoggingLevel.DEBUG, logger, "NLP response code: ${header.CamelHttpResponseCode}")
            .unmarshal().json()
            .log(LoggingLevel.DEBUG, logger, "NLP response body: ${body}")

            .process(new MetaDataProcessor(getRoutePropertyNamespace()))
            .to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI)

            .stop()
        ;

    }
}
