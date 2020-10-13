/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Test utilities
 */
public class TestUtils {

    private static Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Loads a properties file by filename.
     *
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

    /**
     * Loads a sample message from test resources, returning a {@link File} for use in unit tests.
     *
     * @param messageDirectory The message subdirectory under src/test/resources/messages. Example: hl7, fhir, etc.
     * @param messageName      The name of the message file.
     * @return {@link File}
     * @throws URISyntaxException    if an error occurs loading the message file
     * @throws FileNotFoundException if the message file is not found
     */
    public static File getMessage(String messageDirectory, String messageName) throws URISyntaxException,
            IOException {
        String messagePath = "messages/" + messageDirectory + "/" + messageName;

        Path filePath = Paths.get(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource(messagePath).toURI());

        File messageFile = filePath.toFile();

        if (!messageFile.exists()) {
            throw new FileNotFoundException("unable to load " + messageFile.getAbsolutePath());
        }
        return messageFile;
    }

    /**
     * Loads  sample message data from test resources, returning a {@link List<String>} of data, for use in unit tests.
     *
     * @param messageDirectory The message subdirectory under src/test/resources/messages. Example: hl7, fhir, etc.
     * @param messageName      The name of the message file.
     * @return {@link List<String>}
     * @throws URISyntaxException if an error occurs loading the message file
     * @throws IOException        if the message file is not found or an error occurs reading the file
     */
    public static List<String> getMessageAsList(String messageDirectory, String messageName) throws URISyntaxException,
            IOException {
        File messageFile = getMessage(messageDirectory, messageName);
        return Files.readAllLines(messageFile.toPath());
    }

    /**
     * Loads  sample message data from test resources, returning a {@link String} of data, for use in unit tests.
     *
     * @param messageDirectory The message subdirectory under src/test/resources/messages. Example: hl7, fhir, etc.
     * @param messageName      The name of the message file.
     * @param delimiter        The delimiter used to "join" the separate message lines
     * @return {@link List<String>}
     * @throws URISyntaxException if an error occurs loading the message file
     * @throws IOException        if the message file is not found or an error occurs reading the file
     */
    public static String getMessageAsString(String messageDirectory, String messageName, String delimiter) throws
            IOException, URISyntaxException {
        List<String> message = getMessageAsList(messageDirectory, messageName);
        return String.join(delimiter, message);
    }
}
