/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support.x12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Splits a X12 message/batch into transactions.
 * A single transaction contains a single contract, or unit of work.
 */
public final class X12TransactionSplitter {

    private final static String INTERCHANGE_HEADER_SEGMENT = "ISA";

    private final static String INTERCHANGE_FOOTER_SEGMENT = "IEA";

    private final static String FUNCTIONAL_GROUP_HEADER_SEGMENT = "GS";

    private final static String FUNCTIONAL_GROUP_FOOTER_SEGMENT = "GE";

    private final static String TRANSACTION_HEADER_SEGMENT = "ST";

    private final static String TRANSACTION_FOOTER_SEGMENT = "SE";

    private String fieldDelimiter;

    private String lineSeparator;

    private Map<String, String> segmentCache = new HashMap<>();

    /**
     * Sets X12 message delimiters using the ISA/interchange segment
     * @param x12Data The X12 data message/batch
     */
    private void setX12Delimiters(String x12Data) {
        String isaSegment = x12Data.substring(0, IsaValidatingParser.ISA_SEGMENT_LENGTH);
        IsaValidatingParser isaParser = new IsaValidatingParser(isaSegment);
        fieldDelimiter = isaParser.getFieldDelimiter();
        lineSeparator = isaParser.getLineSeparator();
    }

    /**
     * Writes a x12 transaction, including "control segments" (ISA, GS, ST segments) to the transaction list.
     * The working transaction is cleared once it is written.
     * @param sourceTransactionSegments The current transaction {@link List<String> of segments} to "write"
     * @param targetTransactionList The "master" transaction list for the X12 message.
     */
    private void writeTransaction(List<String> sourceTransactionSegments, List<String> targetTransactionList) {
        String transactionData = segmentCache.get(INTERCHANGE_HEADER_SEGMENT) + lineSeparator +
                segmentCache.get(FUNCTIONAL_GROUP_HEADER_SEGMENT) + lineSeparator +
                segmentCache.get(TRANSACTION_HEADER_SEGMENT) + lineSeparator +
                String.join(lineSeparator, sourceTransactionSegments) + lineSeparator;

        targetTransactionList.add(transactionData);
        sourceTransactionSegments.clear();
    }

    /**
     * Splits the X12 Data Message/Batch into separate transactions.
     * Selected control segments (ISA, GS, ST segments) are included for metadata and provenance.
     * @param x12Data The source message/batch
     * @return {@link List<String>} of messages
     */
    public List<String> split(String x12Data) {
        List<String> x12Transactions = new ArrayList<>();

        setX12Delimiters(x12Data);

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
                    writeTransaction(x12Transaction, x12Transactions);
                    break;

                default:
                    x12Transaction.add(segment);
            }
        }
        return x12Transactions;
    }
}
