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
 * Test utilities
 */
public class TestUtils {

    private static Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Loads a properties file by filename.
     * @param filename to load
     * @return {@link Properties} instance
     * @throws IOException if an error occurs reading application.properties
     */
    public static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();

        Path path = Paths.get(filename);

        if (Files.exists(path)) {
            properties.load(Files.newInputStream(path));
            String absolutePath = path.toAbsolutePath().toString();
            logger.info("loading properties from file:{}", absolutePath);
        } else {
            properties.load(ClassLoader.getSystemResourceAsStream(filename));
            logger.info("loading properties from classpath:{}", filename);
        }

        return properties;
    }
}
