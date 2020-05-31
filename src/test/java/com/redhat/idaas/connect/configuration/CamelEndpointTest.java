package com.redhat.idaas.connect.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Tests {@link CamelEndpoint}
 */
public class CamelEndpointTest {

    private CamelEndpoint camelEndpoint;

    /**
     * Constructs a {@link CamelEndpoint} fixture prior to each test
     */
    @BeforeEach
    public void beforeEach() {
        camelEndpoint = new CamelEndpoint();
        camelEndpoint.setScheme("ftp://");
        camelEndpoint.setContextPath("myftp.com:21/dropbox");
    }

    /**
     * Tests {@link CamelEndpoint#equals} where an instance is compared to itself
     */
    @Test
    public void testEqualsSameInstance() {
        CamelEndpoint otherEndpoint = camelEndpoint;
        Assertions.assertEquals(camelEndpoint, otherEndpoint);
    }

    /**
     * Tests {@link CamelEndpoint#equals} where an instance is compared to null
     */
    @Test
    public void testEqualsNull() {
        Assertions.assertNotEquals(camelEndpoint, null);
    }

    /**
     * Tests {@link CamelEndpoint#equals} where an instance is compared to a non-compatible type
     */
    @Test
    public void testEqualsDifferentClass() {
        Assertions.assertNotEquals(camelEndpoint, "a string");
    }

    /**
     * Tests {@link CamelEndpoint#equals} where an instance is compared to an equivalent object
     */
    @Test
    public void testEqualsEquivalent() {
        CamelEndpoint otherEndpoint = new CamelEndpoint();
        otherEndpoint.setScheme(camelEndpoint.getScheme());
        otherEndpoint.setContextPath(camelEndpoint.getContextPath());
        Assertions.assertEquals(camelEndpoint, otherEndpoint);
    }

    /**
     * Tests {@link CamelEndpoint#equals} where an instance is compared to a non-equivalent object
     */
    @Test
    public void testEqualsNotEquivalent() {
        CamelEndpoint otherEndpoint = new CamelEndpoint();
        otherEndpoint.setScheme(camelEndpoint.getScheme());
        otherEndpoint.setContextPath("myftp.com:21/dropoff");
        Assertions.assertNotEquals(camelEndpoint, otherEndpoint);
    }

    /**
     * Validates that {@link CamelEndpoint#hashCode()} generates the same hash code for equivalent objects
     */
    @Test
    public void testHashCodeForEqualObjects() {
        CamelEndpoint otherEndpoint = new CamelEndpoint();
        otherEndpoint.setScheme(camelEndpoint.getScheme());
        otherEndpoint.setContextPath(camelEndpoint.getContextPath());

        Assertions.assertEquals(camelEndpoint.hashCode(), otherEndpoint.hashCode());
    }

    /**
     * Validates that {@link CamelEndpoint#hashCode()} generates the same hash code for non-equivalent objects
     */
    @Test
    public void testHashCodeForNonEqualObjects() {
        CamelEndpoint otherEndpoint = new CamelEndpoint();
        otherEndpoint.setScheme(camelEndpoint.getScheme());
        otherEndpoint.setContextPath("myftp.com:21/dropoff");

        Assertions.assertNotEquals(camelEndpoint.hashCode(), otherEndpoint.hashCode());
    }

    /**
     * Tests {@link CamelEndpoint#toString()}
     */
    @Test
    public void testToString() {
        String expectedString = "ftp://myftp.com:21/dropbox";
        Assertions.assertEquals(expectedString, camelEndpoint.toString());
    }

    /**
     * Tests {@link CamelEndpoint#toString()} when endpoint options are present
     */
    @Test
    public void testToStringWithOptions() {
        camelEndpoint.setOptions("binary=true&disconnect=true&transferLoggingLevel=ERROR");

        String expectedUri = "ftp://myftp.com:21/dropbox?binary=true&disconnect=true&transferLoggingLevel=ERROR";
        String actualUri = camelEndpoint.toString();
        Assertions.assertEquals(expectedUri, actualUri);
    }
}
