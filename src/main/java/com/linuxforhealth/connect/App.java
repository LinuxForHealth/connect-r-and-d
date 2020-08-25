/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect;

import com.linuxforhealth.connect.support.NATSSubscriberManager;
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
    private final static String ENV_NAMESPACE = "lfh_connect_";
    private final static String BEAN_PROPERTY_NAMESPACE = "lfh.connect.bean";
    private final static String ROUTE_BUILDER_PACKAGE = "com.linuxforhealth.connect.builder";

    private final Logger logger = LoggerFactory.getLogger(App.class);

    private final Main camelMain = new Main();

    /**
     * Binds Camel processing beans, to the Camel registry
     * @param appProperties The application properties
     * @throws ReflectiveOperationException if an error occurs creating new component instances
     */
    private void bindBeans(Properties appProperties) throws ReflectiveOperationException {

        Set<String> beanPropertyKeys = appProperties
                .stringPropertyNames()
                .stream()
                .filter(prop -> prop.startsWith(App.BEAN_PROPERTY_NAMESPACE))
                .collect(Collectors.toSet());

        for (String beanPropertyKey : beanPropertyKeys) {
            String beanName = beanPropertyKey.replace(App.BEAN_PROPERTY_NAMESPACE.concat("."), "");
            String beanClass = appProperties.getProperty(beanPropertyKey);

            logger.debug("adding bean name = {}, class = {} to registry", beanName, beanClass);

            Constructor<?> componentConstructor = Class.forName(beanClass).getConstructor();
            camelMain.bind(beanName, componentConstructor.newInstance());
        }
    }

    /**
     * Loads properties from a well known external location in {@link App#EXTERNAL_PROPERTY_FILE_PATH}
     * @return {@link Properties} object
     * @throws IOException if an error occurs reading the properties from file
     */
    private Properties loadExternalProperties() throws IOException {
        Path externalPropertyPath = Paths.get(App.EXTERNAL_PROPERTY_FILE_PATH);
        Properties externalProperties = new Properties();

        if (Files.exists(externalPropertyPath)) {
            String absolutePath = externalPropertyPath.toAbsolutePath().toString();
            logger.info("loading override properties from file:{}", absolutePath);
            externalProperties.load(Files.newInputStream(externalPropertyPath));
        }
        return externalProperties;
    }

    /**
     * Loads property settings from environment variables.
     * Properties are translated from environment variable names and values.
     * @return {@link Properties} object.
     */
    private Properties loadEnvironmentProperties() {
        Properties envProperties = new Properties();

        System.getenv()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(ENV_NAMESPACE))
                .forEach(e -> {
                    String key = e.getKey().toLowerCase().replaceAll("_", ".");
                    String value = e.getValue();
                    envProperties.put(key, value);
                });

        return envProperties;
    }

    /**
     * Loads application properties from the classpath, external file, and environment variables.
     *
     * Properties are resolved in the following order:
     * <ul>
     *     <li>classpath</li>
     *     <li>external file</li>
     *     <li>environment variables</li>
     * </ul>
     *
     * Environment variables are translated to a valid "property" format.
     * Example: LFH_CONNECT_FOO=bar is translated to lfh.connect.foo=bar.
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
        camelPropertiesComponent.setLocation("classpath:" + App.APPLICATION_PROPERTIES_FILE_NAME);

        Properties externalProperties = loadExternalProperties();
        if (externalProperties.entrySet().size() > 0) {
            camelPropertiesComponent.setOverrideProperties(externalProperties);
            externalProperties.forEach((k, v) -> {
                properties.setProperty(k.toString(), v.toString());
            });
        }

        Properties envProperties = loadEnvironmentProperties();
        if (envProperties.entrySet().size() > 0) {
            camelPropertiesComponent.setOverrideProperties(envProperties);
            envProperties.forEach((k, v) -> {
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
            logger.info("starting NATS subscribers");
            NATSSubscriberManager.startSubscribers(appProperties);

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
