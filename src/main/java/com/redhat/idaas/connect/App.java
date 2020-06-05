package com.redhat.idaas.connect;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The iDAAS Connect application.
 * iDAAS Connect provides data integration and processing routes for application integration.
 * Apache Camel is use to provide routing, mediation, and processing services for data integrations.
 *
 * Application settings are stored in an application.properties file, located on the classpath.
 * Camel Routes are defined within the {@link App#IDAAS_ROUTES_PACKAGE} which is scanned on application startup.
 */
public final class App {

    private final static String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
    private final static String COMPONENT_PROPERTY_NAMESPACE = "idaas.connect.component";
    private final static String IDAAS_ROUTES_PACKAGE = "com.redhat.idaas.connect.routes";

    private final Logger logger = LoggerFactory.getLogger(App.class);

    private final Main camelMain = new Main();

    /**
     * Binds Camel components, or beans, to Camel's registry
     *
     * @throws ReflectiveOperationException if an error occurs creating new component instances
     */
    private void bindBeans() throws IOException, ReflectiveOperationException {

        Properties properties = loadProperties();

        Set<String> componentPropertyKeys = properties
                .stringPropertyNames()
                .stream()
                .filter(prop -> prop.startsWith(App.COMPONENT_PROPERTY_NAMESPACE))
                .collect(Collectors.toSet());

        for (String componentKey : componentPropertyKeys) {
            String componentName = componentKey.replace(App.COMPONENT_PROPERTY_NAMESPACE.concat("."), "");
            String componentClass = properties.getProperty(componentKey);

            logger.debug("adding component name = {} value = {}", componentName, componentClass);

            Constructor<?> componentConstructor = Class.forName(componentClass).getConstructor();
            camelMain.bind(componentName, componentConstructor.newInstance());
        }
    }

    /**
     * Loads application properties from the classpath
     *
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(App.APPLICATION_PROPERTIES_FILE_NAME)) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Configures the application prior to starting Camel services
     * <ol>
     *     <li>Binds components to the Camel Registry</li>
     *     <li>Adds routes to the Camel Application</li>
     * </ol>
     * @throws IOException if an error occurs reading application.properties
     * @throws ReflectiveOperationException if an error occurs creating Camel component instances
     */
    private void configure() throws IOException, ReflectiveOperationException {
        bindBeans();
        camelMain.configure().withPackageScanRouteBuilders(IDAAS_ROUTES_PACKAGE);
    }

    /**
     * Starts the iDAAS Connect application
     */
    private void start()  {
        try {

            logger.info("configuring camel context");
            configure();
            logger.info("starting camel context");
            camelMain.start();

        } catch (Exception ex) {
            logger.error("an error occurred starting idaas-connect", ex);

            if (camelMain.isStarted()) {
                camelMain.shutdown();
            }
        }
    }

    /**
     * Entry-ooint for iDAAS Connection Application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        App app = new App();
        app.start();
    }
}
