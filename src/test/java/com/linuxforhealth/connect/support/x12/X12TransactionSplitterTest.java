/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support.x12;

import com.linuxforhealth.connect.support.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link X12TransactionSplitter}
 */
public class X12TransactionSplitterTest {

    private X12TransactionSplitter x12Splitter = new X12TransactionSplitter();

    @Test
    void testSingle270Transaction() throws Exception {
        String expectedTransactionData = TestUtils.getMessageAsString("x12/splitter", "270-005010X279A1.expected.txt", "");

        String x12Data = TestUtils.getMessageAsString("x12/splitter", "270-005010X279A1.txt", "");
        List<String> actualTransactions = x12Splitter.split(x12Data);

        Assertions.assertEquals(1, actualTransactions.size());
        Assertions.assertEquals(expectedTransactionData, actualTransactions.get(0));
    }

    @Test
    void testMultipleTransactions() throws Exception {
        List<String> expectedTransactions = new ArrayList<>();
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.1.txt", ""));
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.2.txt", ""));
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.3.txt", ""));

        String x12Data = TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.txt", "");
        List<String> actualTransactions = x12Splitter.split(x12Data);

        Assertions.assertEquals(3, actualTransactions.size());
        Assertions.assertEquals(expectedTransactions.get(2), actualTransactions.get(2));
    }

}
