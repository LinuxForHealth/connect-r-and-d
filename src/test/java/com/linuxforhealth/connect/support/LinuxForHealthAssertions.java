/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.junit.jupiter.api.Assertions;

import java.util.Arrays;

/**
 * Provides LinuxForHealth test case assertions
 */
public class LinuxForHealthAssertions {

    /**
     * Determines if two endpoint URIs are the "same" which ignores component option order.
     *
     * @param expectedUri The expected URI
     * @param actualUri The actual URI
     */
    public static void assertEndpointUriSame(String expectedUri, String actualUri) {
        Assertions.assertEquals(expectedUri.length()
                , actualUri.length()
                , "URI length is not equal expected uri length = " + expectedUri.length() +
                        " actual uri length = " + actualUri.length());

        String[] expectedTokens = expectedUri.split("\\?");
        String[] actualTokens = actualUri.split("\\?");

        Assertions.assertEquals(expectedTokens.length, actualTokens.length,
                "URI option mismatch expected uri = " + expectedUri +
                        "actual uri = "+ actualUri);

        Assertions.assertEquals(expectedTokens[0], actualTokens[0],
                "URI base components are not equal. expected base uri = " + expectedTokens[0] +
                        "actual base uri = " + actualTokens[0]);

        if (expectedTokens.length > 1) {
            String[] expectedQuery = expectedTokens[1].split("&");
            Arrays.sort(expectedQuery);

            String[] actualQuery = actualTokens[1].split("&");
            Arrays.sort(actualQuery);

            Assertions.assertArrayEquals(expectedQuery, actualQuery);
        }
    }
}
