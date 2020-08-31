/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Linux for Health utilities
 */
public class LFHUtils {

    private static Logger logger = LoggerFactory.getLogger(LFHUtils.class);

    private final static String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
    private final static String EXTERNAL_PROPERTY_FILE_PATH = "config/application.properties";
    private final static String ENV_NAMESPACE = "lfh_connect_";

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
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    public static Properties loadProperties() {
        Properties properties = new Properties();

        try {
            properties.load(ClassLoader.getSystemResourceAsStream(APPLICATION_PROPERTIES_FILE_NAME));
            logger.debug("loading properties from classpath:{}", APPLICATION_PROPERTIES_FILE_NAME);

            Properties externalProperties = loadExternalProperties();
            if (externalProperties.entrySet().size() > 0) {
                externalProperties.forEach((k, v) -> {
                    properties.setProperty(k.toString(), v.toString());
                });
            }

            Properties envProperties = loadEnvironmentProperties();
            if (envProperties.entrySet().size() > 0) {
                envProperties.forEach((k, v) -> {
                    properties.setProperty(k.toString(), v.toString());
                });
            }
        } catch (IOException ex) {
            logger.error("IOException: "+ex);
        } 

        return properties;
    }

    /**
     * Loads properties from a well known external location in {@link App#EXTERNAL_PROPERTY_FILE_PATH}
     * @return {@link Properties} object
     * @throws IOException if an error occurs reading the properties from file
     */
    private static Properties loadExternalProperties() throws IOException {
        Path externalPropertyPath = Paths.get(EXTERNAL_PROPERTY_FILE_PATH);
        Properties externalProperties = new Properties();

        if (Files.exists(externalPropertyPath)) {
            String absolutePath = externalPropertyPath.toAbsolutePath().toString();
            logger.debug("loading override properties from file:{}", absolutePath);
            externalProperties.load(Files.newInputStream(externalPropertyPath));
        }
        return externalProperties;
    }

    /**
     * Loads property settings from environment variables.
     * Properties are translated from environment variable names and values.
     * @return {@link Properties} object.
     */
    private static Properties loadEnvironmentProperties() {
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
}
