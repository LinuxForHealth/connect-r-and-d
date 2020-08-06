/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Tests {@link LinuxForHealthAssertions}
 */
public class LinuxForHealthAssertionsTest {

    /**
     * Tests {@link LinuxForHealthAssertions#assertEndpointUriSame(String, String)}
     */
    @Test
    void testAssertEndpointUrisSame() {
        String expected = "netty://tcp://0.0.0.0:2525";
        String actual = "netty://tcp://0.0.0.0:2525";
        LinuxForHealthAssertions.assertEndpointUriSame(expected, actual);

        expected += "?decoder=decoder&encoder=encoder";
        actual += "?decoder=decoder&encoder=encoder";
        LinuxForHealthAssertions.assertEndpointUriSame(expected, actual);

        actual = "netty://tcp://0.0.0.0:2525?encoder=encoder&decoder=decoder";
        LinuxForHealthAssertions.assertEndpointUriSame(expected, actual);
    }

    /**
     * Tests @link LinuxForHealthAssertions#assertEndpointUriSame(String, String)} where base uri components are different
     */
    @Test
    void testAssertEndpointUrisSameBaseUriDiff() {
        String expected = "netty://tcp://0.0.0.0:2525?encoder=encoder&decoder=decoder";
        String actual = "tcp://0.0.0.0:2525?encoder=encoder&decoder=decoder";
        Assertions.assertThrows(AssertionFailedError.class,
                () -> LinuxForHealthAssertions.assertEndpointUriSame(expected, actual));
    }

    /**
     * Tests @link LinuxForHealthAssertions#assertEndpointUriSame(String, String)} where query components are different
     */
    @Test
    void testAssertEndpointUrisSameQueryDiff() {
        String expected = "netty://tcp://0.0.0.0:2525?encoder=encoder&decoder=decoder";
        String actual = "nett://tcp://0.0.0.0:2525?encoder=encoder&foo=bar";
        Assertions.assertThrows(AssertionFailedError.class,
                () -> LinuxForHealthAssertions.assertEndpointUriSame(expected, actual));
    }
}
