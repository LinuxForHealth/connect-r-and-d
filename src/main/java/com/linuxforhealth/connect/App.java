/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect;

import com.linuxforhealth.connect.configuration.EndpointUriBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Linux For Health Connect application.
 * Linux For Health Connect provides data integration and processing routes for application integration.
 * Apache Camel is use to provide routing, mediation, and processing services for data integrations.
 *
 * Application settings are stored in an application.properties file, located on the classpath.
 * Camel Routes are defined within the {@link App#ROUTE_BUILDER_PACKAGE} which is scanned on application startup.
 */
public final class App {

    private final static String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
    private final static String EXTERNAL_PROPERTY_FILE_PATH = "config/application.properties";
    private final static String COMPONENT_PROPERTY_NAMESPACE = "linuxforhealth.connect.component";
    private final static String ROUTE_BUILDER_PACKAGE = "com.linuxforhealth.connect.builder";

    private final Logger logger = LoggerFactory.getLogger(App.class);

    private final Main camelMain = new Main();

    /**
     * Binds Camel components, or beans, to Camel's registry
     * @param appProperties The application properties
     * @throws ReflectiveOperationException if an error occurs creating new component instances
     */
    private void bindBeans(Properties appProperties) throws ReflectiveOperationException {

        Set<String> componentPropertyKeys = appProperties
                .stringPropertyNames()
                .stream()
                .filter(prop -> prop.startsWith(App.COMPONENT_PROPERTY_NAMESPACE))
                .collect(Collectors.toSet());

        for (String componentKey : componentPropertyKeys) {
            String componentName = componentKey.replace(App.COMPONENT_PROPERTY_NAMESPACE.concat("."), "");
            String componentClass = appProperties.getProperty(componentKey);

            logger.debug("adding component name = {} value = {} to registry", componentName, componentClass);

            Constructor<?> componentConstructor = Class.forName(componentClass).getConstructor();
            camelMain.bind(componentName, componentConstructor.newInstance());
        }

        logger.debug("adding endpoint uri builder to registry");
        camelMain.bind(EndpointUriBuilder.BEAN_NAME, new EndpointUriBuilder(appProperties));
    }

    /**
     * Loads application properties.
     *
     * Properties are loaded from the classpath and may be overridden using an external file located in
     * config/application.properties.
     *
     * Properties are registered with the camel context and returned from this method for bootstrap processing.
     * This "double-evaluation" is required as the app has to configure some components prior to the camel context
     * being available.
     *
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        PropertiesComponent camelPropertiesComponent = configurePropertyParser();

        properties.load(ClassLoader.getSystemResourceAsStream(App.APPLICATION_PROPERTIES_FILE_NAME));
        logger.info("loading properties from classpath:{}", App.APPLICATION_PROPERTIES_FILE_NAME);
        camelPropertiesComponent.setLocation("classpath:"+App.APPLICATION_PROPERTIES_FILE_NAME);

        Path externalPropertyPath = Paths.get(App.EXTERNAL_PROPERTY_FILE_PATH);

        if (Files.exists(externalPropertyPath)) {
            String absolutePath = externalPropertyPath.toAbsolutePath().toString();
            logger.info("loading override properties from file:{}", absolutePath);

            Properties overrideProperties = new Properties();
            overrideProperties.load(Files.newInputStream(externalPropertyPath));
            camelPropertiesComponent.setOverrideProperties(overrideProperties);

            // set override properties
            overrideProperties.forEach((k, v) -> {
                properties.setProperty(k.toString(), v.toString());
            });
        }

        return properties;
    }

    /**
     * Configures encrypted property parsing using PropertiesComponent and Jasypt for encryption.
     *
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    private PropertiesComponent configurePropertyParser() throws IOException {
        // LFH default master password (development only)
        String password = "ultrasecret";
        // New password, if set
        String newPassword = System.getenv("JASYPT_ENCRYPTION_PASSWORD");

        if (newPassword != null) {
            logger.info("Using JASYPT_ENCRYPTION_PASSWORD as the master password");
            password = newPassword;
        }

        // Configure the Jasypt master password
        JasyptPropertiesParser jasypt = new JasyptPropertiesParser();
        jasypt.setPassword(password);

        // Configure the PropertiesComponent to use Jasypt
        PropertiesComponent pc = new PropertiesComponent();
        pc.setPropertiesParser(jasypt);

        camelMain.bind("properties", pc);

        return pc;
    }

    /**
     * Configures the application prior to starting Camel services.
     * <ol>
     *     <li>Binds components to the Camel Registry</li>
     *     <li>Adds routes to the Camel Application</li>
     * </ol>
     * @throws ReflectiveOperationException if an error occurs creating Camel component instances
     */
    private void configure(Properties appProperties) throws ReflectiveOperationException {
        bindBeans(appProperties);
        logger.debug("scanning and discovering routes in {}", ROUTE_BUILDER_PACKAGE);
        camelMain.configure().withPackageScanRouteBuilders(ROUTE_BUILDER_PACKAGE);
    }

    /**
     * Starts the Linux for Health Connect application
     */
    private void start()  {
        try {
            Properties appProperties = loadProperties();
            logger.info("configuring camel context");
            configure(appProperties);
            logger.info("starting camel context");
            camelMain.start();

        } catch (Exception ex) {
            logger.error("an error occurred starting linux for health connect", ex);

            if (camelMain.isStarted()) {
                logger.error("shutting down camel context");
                camelMain.shutdown();
            }
        }
    }

    /**
     * Entry-point for Linux For Health Connection Application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        App app = new App();
        app.start();
    }
}
