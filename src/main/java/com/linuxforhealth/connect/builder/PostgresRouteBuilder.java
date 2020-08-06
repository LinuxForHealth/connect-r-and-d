package com.linuxforhealth.connect.builder;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresRouteBuilder extends RouteBuilder {
	
	public final static String POSTGRES_ROUTE_ID = "postgres";
	
	private final Logger logger = LoggerFactory.getLogger(PostgresRouteBuilder.class);

	public PostgresRouteBuilder() {
		// TODO Auto-generated constructor stub
	}

	public PostgresRouteBuilder(CamelContext context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void configure() throws Exception {
		
		from("direct:postgres")
		.routeId(POSTGRES_ROUTE_ID)
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		.to("jdbc:datasource")
		;
		
	}

}
