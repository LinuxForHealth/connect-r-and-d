package com.linuxforhealth.connect.builder;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgreSQL JDBC Route
 * INPUT: SQL statements in message body
 * OUTPUT: Configured data source
 */
public class PostgresRouteBuilder extends RouteBuilder {
	
	public final static String POSTGRES_ROUTE_ID = "postgres";
	
	// Data source object
	@BeanInject() BasicDataSource ds;
	
	// Data source properties
	@PropertyInject("lfh.connect.pgds.url") private String pg_url;
	@PropertyInject("lfh.connect.pgds.user") private String pg_user;
	@PropertyInject("lfh.connect.pgds.driver") private String pg_driver;
	
	private final Logger logger = LoggerFactory.getLogger(PostgresRouteBuilder.class);
	

	public PostgresRouteBuilder() { }

	public PostgresRouteBuilder(CamelContext context) {
		super(context);
	}

	@Override
	public void configure() throws Exception {
		
		if (ds != null) {
			ds.setUrl(pg_url);
			ds.setUsername(pg_user);
			ds.setDriverClassName(pg_driver);
			logger.debug("Data source url: {} user: {} driver: {}", pg_url, pg_user, pg_driver);
			
		} else { logger.warn("Data source bean not found in registry"); }
		
		from("direct:postgres")
		.routeId(POSTGRES_ROUTE_ID)
		.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}")
		.to("jdbc:pgds")
		.log(LoggingLevel.DEBUG, logger, "Query output: ${body}")
		;
		
	}

}
