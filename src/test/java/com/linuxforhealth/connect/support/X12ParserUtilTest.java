/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link X12ParserUtil}
 */
public class X12ParserUtilTest {

    private X12ParserUtil x12ParserUtil = new X12ParserUtil();

    @Test
    void testSplitSingle270Transaction() throws Exception {
        String expectedTransactionData = TestUtils.getMessageAsString("x12/splitter", "270-005010X279A1.expected.txt", "");

        String x12Data = TestUtils.getMessageAsString("x12/splitter", "270-005010X279A1.txt", "");
        List<String> actualTransactions = x12ParserUtil.split(x12Data, "*", "~");

        Assertions.assertEquals(1, actualTransactions.size());
        Assertions.assertEquals(expectedTransactionData, actualTransactions.get(0));
    }

    @Test
    void testSplitMultipleTransactions() throws Exception {
        List<String> expectedTransactions = new ArrayList<>();
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.1.txt", ""));
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.2.txt", ""));
        expectedTransactions.add(TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.expected.3.txt", ""));

        String x12Data = TestUtils.getMessageAsString("x12/splitter", "270-837-005010X279A1.txt", "");
        List<String> actualTransactions = x12ParserUtil.split(x12Data, "*", "~");

        Assertions.assertEquals(3, actualTransactions.size());
        Assertions.assertEquals(expectedTransactions.get(0), actualTransactions.get(0));
        Assertions.assertEquals(expectedTransactions.get(1), actualTransactions.get(1));
        Assertions.assertEquals(expectedTransactions.get(2), actualTransactions.get(2));
    }

    @Test
    void testGetX12MessageType() {
        String actualMessageType = x12ParserUtil.getX12MessageType("ST*270*0001*005010X279A1~", "*", "~");
        Assertions.assertEquals("270", actualMessageType);

        actualMessageType = x12ParserUtil.getX12MessageType("ST|270|0001|005010X279A1*", "|", "*");
        Assertions.assertEquals("270", actualMessageType);

        actualMessageType = x12ParserUtil.getX12MessageType("ISA*00*          *00*          *ZZ*890069730      *ZZ*154663145      *200929*1705*|*00501*000000001*0*T*:~GS*HS*890069730*154663145*20200929*1705*0001*X*005010X279A1~ST*270*0001*005010X279A1~BHT*0022*13*10001234*20200929*1319~", "*", "~");
        Assertions.assertEquals("270", actualMessageType);
    }
}
