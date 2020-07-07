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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.OpenOption;
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
     * Properties are loaded from an external file if available, otherwise properties are loaded from
     * the classpath. If an external file is available, the camel context is updated to use it as the
     * default properties file for the application.
     *
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        PropertiesComponent pc = configurePropertyParser();

        Path path = Paths.get(App.EXTERNAL_PROPERTY_FILE_PATH);

        if (Files.exists(path)) {
            properties.load(Files.newInputStream(path));
            String absolutePath = path.toAbsolutePath().toString();
            logger.info("loading properties from file:{}", absolutePath);
            camelMain.setDefaultPropertyPlaceholderLocation("file:" + absolutePath);
            pc.setLocation(absolutePath);
        } else {
            properties.load(ClassLoader.getSystemResourceAsStream(App.APPLICATION_PROPERTIES_FILE_NAME));
            logger.info("loading properties from classpath:{}", App.APPLICATION_PROPERTIES_FILE_NAME);
            pc.setLocation("classpath:"+APPLICATION_PROPERTIES_FILE_NAME);
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
        // Configure the Jasypt master password
        // TODO: use env var, e.g. sysenv:CAMEL_ENCRYPTION_PASSWORD
        JasyptPropertiesParser jasypt = new JasyptPropertiesParser();
        jasypt.setPassword("ultrasecret");

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
