package com.linuxforhealth.connect.support.x12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Splits a X12 message/batch into transactions.
 * A single transaction contains a single contract, or unit of work.
 */
public final class X12TransactionSplitter {

    private final Predicate<String> isIncludedSegment = s -> !s.startsWith("ISA") &&
            !s.startsWith("ISE") &&
            !s.startsWith("GE");

    private final static String FUNCTIONAL_GROUP_HEADER_SEGMENT = "GS";
    private final static String TRANSACTION_HEADER_SEGMENT = "ST";
    private final static String TRANSACTION_FOOTER_SEGMENT = "SE";

    private String fieldDelimiter;
    private String lineSeparator;

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
     * Splits the X12 Data Message/Batch into separate transactions.
     * The Functional Group Header (GS Segment) and Transaction Header (ST segment) are included to preserve metadata.
     * @param x12Data The source message/batch
     * @return {@link List<String>} of messages
     */
    public List<String> split(String x12Data) {
        List<String> x12Transactions = new ArrayList<>();

        setX12Delimiters(x12Data);

        // filter metadata segments to simplify processing
        List<String> x12Segments = Arrays
                .stream(x12Data.split(lineSeparator))
                .filter(isIncludedSegment)
                .collect(Collectors.toList());

        List<String> x12Transaction = new ArrayList<>();

        String segmentType = null;
        String functionalGroupHeader = null;
        String transactionHeader = null;

        for (String segment: x12Segments) {
            segmentType = segment.substring(0, segment.indexOf(fieldDelimiter));

            switch (segmentType.toUpperCase()) {
                case FUNCTIONAL_GROUP_HEADER_SEGMENT:
                    functionalGroupHeader = segment;
                    break;

                case TRANSACTION_HEADER_SEGMENT:
                    transactionHeader = segment;
                    break;

                case TRANSACTION_FOOTER_SEGMENT:
                    String transactionData = functionalGroupHeader + lineSeparator;
                    transactionData += transactionHeader + lineSeparator;
                    transactionData += String.join(lineSeparator, x12Transaction) + lineSeparator;
                    x12Transactions.add(transactionData);

                    x12Transaction.clear();
                    break;

                default:
                    x12Transaction.add(segment);
            }
        }
        return x12Transactions;
    }
}
