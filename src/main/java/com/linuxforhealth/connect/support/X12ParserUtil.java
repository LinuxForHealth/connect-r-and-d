/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides convenience functions for common X12 parsing operations.
 */
public final class X12ParserUtil {

    private final static String INTERCHANGE_HEADER_SEGMENT = "ISA";

    private final static String INTERCHANGE_FOOTER_SEGMENT = "IEA";

    private final static String FUNCTIONAL_GROUP_HEADER_SEGMENT = "GS";

    private final static String FUNCTIONAL_GROUP_FOOTER_SEGMENT = "GE";

    private final static String TRANSACTION_HEADER_SEGMENT = "ST";

    private final static String TRANSACTION_FOOTER_SEGMENT = "SE";

    private Map<String, String> segmentCache = new HashMap<>();

    /**
     * Writes a split x12 transaction, including "control segments" (ISA, GS, ST segments) to the transaction list.
     * The working transaction is cleared once it is written.
     * @param sourceTransactionSegments The current transaction {@link List<String> of segments} to "write"
     * @param targetTransactionList The "master" transaction list for the X12 message.
     * @param x12LineSeparator The character used to end a line/x12 segment
     */
    private void writeTransaction(List<String> sourceTransactionSegments, List<String> targetTransactionList, String x12LineSeparator) {
        String transactionData = segmentCache.get(INTERCHANGE_HEADER_SEGMENT) + x12LineSeparator +
                segmentCache.get(FUNCTIONAL_GROUP_HEADER_SEGMENT) + x12LineSeparator +
                segmentCache.get(TRANSACTION_HEADER_SEGMENT) + x12LineSeparator +
                String.join(x12LineSeparator, sourceTransactionSegments) + x12LineSeparator;

        targetTransactionList.add(transactionData);
        sourceTransactionSegments.clear();
    }

    /**
     * Splits the X12 Data Message/Batch into separate transactions.
     * Selected control segments (ISA, GS, ST segments) are included for metadata and provenance.
     * @param x12Data The source message/batch
     * @param fieldDelimiter The delimiter used to separate X12 fields
     * @param lineSeparator The ending character for a line
     * @return {@link List<String>} of messages
     */
    public List<String> split(String x12Data, String fieldDelimiter, String lineSeparator) {
        List<String> x12Transactions = new ArrayList<>();

        // filter metadata segments to simplify processing
        List<String> x12Segments = Arrays
                .stream(x12Data.split(lineSeparator))
                .filter(s -> !s.startsWith(INTERCHANGE_FOOTER_SEGMENT)
                        && !s.startsWith(FUNCTIONAL_GROUP_FOOTER_SEGMENT))
                .collect(Collectors.toList());

        List<String> x12Transaction = new ArrayList<>();

        for (String segment: x12Segments) {
            String segmentType = segment.substring(0, segment.indexOf(fieldDelimiter));

            switch (segmentType.toUpperCase()) {
                case INTERCHANGE_HEADER_SEGMENT:
                    segmentCache.put(INTERCHANGE_HEADER_SEGMENT, segment);
                    break;

                case FUNCTIONAL_GROUP_HEADER_SEGMENT:
                    segmentCache.put(FUNCTIONAL_GROUP_HEADER_SEGMENT, segment);
                    break;

                case TRANSACTION_HEADER_SEGMENT:
                    segmentCache.put(TRANSACTION_HEADER_SEGMENT, segment);
                    break;

                case TRANSACTION_FOOTER_SEGMENT:
                    writeTransaction(x12Transaction, x12Transactions, lineSeparator);
                    break;

                default:
                    x12Transaction.add(segment);
            }
        }
        return x12Transactions;
    }

    /**
     * Parses the X12 Message Type from the ST segment transaction code and specification version
     * Example ST Segment:
     * <code>
     *     ST*270*0010*005010X279A1~
     * </code>
     * The transaction code is in the second field, delimited by a "*"
     *
     * @param x12Transaction The X12 transaction string
     * @param fieldDelimiter The field delimiter character
     * @param lineSeparator The line separator character
     * @return The X12 message type for the transaction
     */
    public String getX12MessageType(String x12Transaction, String fieldDelimiter, String lineSeparator) {

        int transactionStart = x12Transaction.indexOf(lineSeparator + "ST") + 1;
        int transactionEnd = x12Transaction.indexOf(lineSeparator, transactionStart);
        String splitCharacter = fieldDelimiter;

        if (splitCharacter.equals("*") || splitCharacter.equals("|") || splitCharacter.equals("?")) {
            splitCharacter = "\\" + splitCharacter;
        }

        String[] transactionHeaderFields = x12Transaction
                .substring(transactionStart, transactionEnd)
                .split(splitCharacter);

        return transactionHeaderFields[1];
    }
}
