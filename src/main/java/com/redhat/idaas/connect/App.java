package com.redhat.idaas.connect;

import com.redhat.idaas.connect.configuration.PropertyParser;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The iDAAS Connect application.
 * iDAAS Connect provides data integration and processing routes for application integration.
 * Apache Camel is use to provide routing, mediation, and processing services for data integrations.
 */
public final class App {

    private static final String PROPERTIES_FILE = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(App.class);

    private final Main camelMain = new Main();

    private final Properties properties = new Properties();

    /**
     * Parses a "component map" to add components to the Camel lookup registry.
     * Component map is structured as:
     * - Key - the component name
     * - Value - component class name
     *
     * @param componentMap {@link Map<>}
     * @throws ReflectiveOperationException if an error occurs creating a component instance
     */
    private void addCamelComponents(Map<String, String> componentMap) throws ReflectiveOperationException {
        for (Map.Entry<String, String> mapEntry : componentMap.entrySet()) {
            String className = mapEntry.getValue();
            Constructor<?> componentConstructor = Class.forName(className).getConstructor();
            camelMain.bind(mapEntry.getKey(), componentConstructor.newInstance());
        }
    }

    /**
     * Configures camel settings, components, and routes.
     *
     * @throws ReflectiveOperationException if an error occurs instantiating components
     */
    private void configureCamel() throws ReflectiveOperationException {
        camelMain.enableHangupSupport();

        PropertyParser propertyParser = new PropertyParser(properties);

        Map<String, String> componentMap = propertyParser.getIdaasComponents();
        addCamelComponents(componentMap);

        List<RouteBuilder> routes = propertyParser.getIdaasRouteDefinitions();
        routes.forEach(r -> camelMain.configure().addRoutesBuilder(r));
    }

    /**
     * Loads application properties from the classpath
     *
     * @param propertyFile The properties file name
     * @throws IOException if an error occurs reading application.properties
     */
    private void loadProperties(String propertyFile) throws IOException {
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertyFile)) {
            properties.load(inputStream);
        }
    }

    /**
     * Parses application command line arguments
     * @param args Command line arguments
     */
    private void parseArguments(String[] args) {}

    /**
     * Starts the iDAAS Connect application
     */
    private void start(String[] args)  {
        try {
            parseArguments(args);
            logger.info("loading properties");
            loadProperties(PROPERTIES_FILE);
            logger.info("configuring camel");
            configureCamel();
            logger.info("starting camel context");
            camelMain.start();
        } catch (IOException|ReflectiveOperationException ex) {
            logger.error("Unable to start iDAAS Connect", ex);

            if (!camelMain.isShutdown()) {
                camelMain.shutdown();
            }
        }
    }

    /**
     * Entrypoint for iDAAS Connection Application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        App app = new App();
        app.start(args);
    }
}
