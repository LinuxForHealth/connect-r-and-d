package com.linuxforhealth.connect.builder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linuxforhealth.connect.processor.MetaDataProcessor;

/**
 * The Class MimicIIIRouteBuilder.
 *
 * @author henry.feldman@ibm.com
 */
public class MimicIIIRouteBuilder extends RouteBuilder {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(MimicIIIRouteBuilder.class);

	/** The Constant MIMIC_III_ROUTE_ID. */
	public final static String MIMIC_III_ROUTE_ID = "mimic-iii";

	/** The Constant MIMIC_III__PRODUCER_ID. */
	public final static String MIMIC_III__PRODUCER_ID = "mimic-iii-producer";

	/** The Constant MIMIC_III_ROUTE_URI. */
	public final static String MIMIC_III_ROUTE_URI = "direct:postgres";

	/** The pg url. */
	// Data source properties
	@PropertyInject("lfh.connect.pgds.url")
	private String pg_url;

	/** The pg user. */
	@PropertyInject("lfh.connect.pgds.user")
	private String pg_user;

	/** The pg pwd. */
	@PropertyInject("lfh.connect.pgds.pwd")
	private String pg_pwd;

	/** The pg driver. */
	@PropertyInject("lfh.connect.pgds.driver")
	private String pg_driver;

	/** The ds. */
	// Data source object
	@BeanInject()
	BasicDataSource ds;

	/** The connection. */
	private Connection connection;

	/**
	 * Gets the route property namespace.
	 *
	 * @return the route property namespace
	 */
	protected String getRoutePropertyNamespace() {
		return "lfh.connect.mimiciii";
	}

	/**
	 * Instantiates a new mimic III route builder.
	 */
	public MimicIIIRouteBuilder() {
		super();
	}

	/**
	 * Instantiates a new mimic III route builder.
	 *
	 * @param context the context
	 */
	public MimicIIIRouteBuilder(CamelContext context) {
		super(context);
	}

	/**
	 * Builds the route.
	 *
	 * @param routePropertyNamespace the route property namespace
	 */
	protected void buildRoute(String routePropertyNamespace) {
		from("{{lfh.connect.mimiciii.uri}}").routeId(MIMIC_III_ROUTE_ID).process(exchange -> {
			String name = simple("${headers.name}").evaluate(exchange, String.class);
			String result = "Hello World! It's " + name + ".";
			// Add your code here
			exchange.getIn().setBody(result);
		}).process(new MetaDataProcessor(routePropertyNamespace))
				.to(LinuxForHealthRouteBuilder.STORE_AND_NOTIFY_CONSUMER_URI).id(MIMIC_III__PRODUCER_ID);

	}

	/**
	 * Configure.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void configure() throws Exception {

		if (ds != null) {
			ds.setUrl(pg_url);
			ds.setUsername(pg_user);
			ds.setPassword(pg_pwd);
			ds.setDriverClassName(pg_driver);
			logger.debug("Data source url: {} user: {} driver: {}", pg_url, pg_user, pg_driver);
			connection = ds.getConnection();
			getPatientFhirResources();

		} else {
			logger.warn("Data source bean not found in registry");
		}

		from(MIMIC_III_ROUTE_URI).routeId(MIMIC_III_ROUTE_ID)
				.log(LoggingLevel.DEBUG, logger, "Received message body: ${body}").to("jdbc:pgds")
				.log(LoggingLevel.DEBUG, logger, "Query output: ${body}");

	}

	/**
	 * Gets the patient list using the chached fhir resources from mimic III
	 *
	 * @return the patient fhir resources
	 */
	private List<String> getPatientFhirResources() {

		List<String> patients = new ArrayList<String>();

		String sql = "select `fhir_json` as fhirJson from patients where `acd_study_patient` = true";
		try {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				java.lang.String fhirJson = resultSet.getString("fhirJson");
				Integer id = resultSet.getInt("subjectId");
				patients.add(fhirJson);
				logger.info("Loaded " + patients.size() + " FHIR patient resources from MIMIC III");
			}
		} catch (Exception e) {
			System.out.println(sql);
			e.printStackTrace();
		}
		return patients;
	}

	private List<String> getPrescriptionFhirResources() {

		List<String> prescriptions = new ArrayList<String>();

		String sql = "select `fhir_json` as fhirJson from prescriptions where `acd_study_patient` = true";
		try {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				java.lang.String fhirJson = resultSet.getString("fhirJson");
				Integer id = resultSet.getInt("subjectId");
				prescriptions.add(fhirJson);
				logger.info("Loaded " + prescriptions.size() + " FHIR MedicationRequest resources from MIMIC III");
			}
		} catch (Exception e) {
			System.out.println(sql);
			e.printStackTrace();
		}
		return prescriptions;
	}

	private List<String> getLabFhirResources() {

		List<String> observations = new ArrayList<String>();

		String sql = "select `fhir_json` as fhirJson from prescriptions where `acd_study_patient` = true";
		try {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				java.lang.String fhirJson = resultSet.getString("fhirJson");
				Integer id = resultSet.getInt("subjectId");
				observations.add(fhirJson);
				logger.info("Loaded " + observations.size() + " FHIR Observation (lab) resources from MIMIC III");
			}
		} catch (Exception e) {
			System.out.println(sql);
			e.printStackTrace();
		}
		return observations;
	}
}
