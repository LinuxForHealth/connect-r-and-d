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

/**
 * Provides convenience functions for common X12 parsing operations.
 */
public final class X12ParserUtil {

  private final static List<String> META_CHARACTERS = Arrays.asList("*", "?", "|");

  private final static String INTERCHANGE_HEADER_SEGMENT = "ISA";

  private final static String INTERCHANGE_FOOTER_SEGMENT = "IEA";

  private final static String FUNCTIONAL_GROUP_HEADER_SEGMENT = "GS";

  private final static String FUNCTIONAL_GROUP_FOOTER_SEGMENT = "GE";

  private final static String TRANSACTION_HEADER_SEGMENT = "ST";

  private final static String TRANSACTION_FOOTER_SEGMENT = "SE";

  // caches ISA, IEA, GS, and GE segments used to form the control envelope
  private final static Map<String, String> controlSegmentCache = new HashMap<>();

  /**
   * Splits a X12 Data Message into separate transactions, returning a list of containing each
   * transaction.
   * <p>
   * Transactions are delimited by the ST (transaction header) and SE (transaction footer) segments.
   * Each transaction is wrapped within the standard control segment envelope, which is structured
   * as ISA, GS, [transaction data], GE, IEA.
   *
   * @param x12Data        The source message
   * @param fieldDelimiter The x12 field delimiter
   * @param lineSeparator  The x12 line ending character
   * @return {@link List<String>} of messages
   */
  public List<String> split(String x12Data, String fieldDelimiter, String lineSeparator) {
    String[] x12Segments = x12Data.split(lineSeparator);
    List<String> currentX12Transaction = new ArrayList<>();
    List<String> splitX12Transactions = new ArrayList<>();

    for (String segment : x12Segments) {
      // segments are structured as [segment type][field delimiter][data]....
      // Example: DTP*358 the segment type is DTP
      String segmentType = segment.substring(0, segment.indexOf(fieldDelimiter)).toUpperCase();

      switch (segmentType) {
        // cache control segment headers
        case INTERCHANGE_HEADER_SEGMENT:
        case FUNCTIONAL_GROUP_HEADER_SEGMENT:
        case TRANSACTION_HEADER_SEGMENT:
          controlSegmentCache.put(segmentType, segment);
          break;

        // SE segment (transaction footer) indicates that the transaction is complete
        case TRANSACTION_FOOTER_SEGMENT:
          controlSegmentCache.put(segmentType, segment);
          String x12Transaction = encloseTransaction(currentX12Transaction, lineSeparator);
          splitX12Transactions.add(x12Transaction);
          currentX12Transaction.clear();
          break;

        // no-op cases for GE and IEA segments in case the control envelope is malformed
        case FUNCTIONAL_GROUP_FOOTER_SEGMENT:
        case INTERCHANGE_FOOTER_SEGMENT:
          break;

        // include "transaction data" segments
        default:
          currentX12Transaction.add(segment);
      }
    }
    return splitX12Transactions;
  }

  /**
   * Parses the X12 Message Type from the ST segment transaction code and specification version
   * Example ST Segment:
   * <code>
   * ST*270*0010*005010X279A1~
   * </code>
   * The transaction code is in the second field, delimited by a "*"
   *
   * @param x12Transaction The X12 transaction string
   * @param fieldDelimiter The field delimiter character
   * @param lineSeparator  The line separator character
   * @return The X12 message type for the transaction
   */
  public String getX12MessageType(String x12Transaction, String fieldDelimiter,
      String lineSeparator) {

    int transactionStart = x12Transaction.indexOf(lineSeparator + "ST") + 1;
    int transactionEnd = x12Transaction.indexOf(lineSeparator, transactionStart);
    String splitCharacter = escapeDelimiter(fieldDelimiter);

    String[] transactionHeaderFields = x12Transaction
        .substring(transactionStart, transactionEnd)
        .split(splitCharacter);

    return transactionHeaderFields[1];
  }

  /**
   * Escapes a delimiter character if it is a regex "meta" character
   *
   * @param delimiter The input delimiter character
   */
  private String escapeDelimiter(String delimiter) {
    return META_CHARACTERS.contains(delimiter) ? "\\" + delimiter : String.valueOf(delimiter);
  }

  /**
   * Creates an Interchange Trailer segment (IEA)
   * The control number from the ISA segment is applied to the IEA segment to ensure that the
   * Interchange Footer is compatible with the Interchange Header.
   * Segment structure:
   * <code>
   * IEA*[number of functional groups]*[control number]~
   * </code>
   */
  private String createInterchangeTrailer() {
    String interchangeHeader = controlSegmentCache.get(INTERCHANGE_HEADER_SEGMENT);
    String fieldDelimiter = interchangeHeader.substring(3, 4);
    String[] interchangeFields = interchangeHeader
        .split(escapeDelimiter(fieldDelimiter));

    String controlNumber = interchangeFields[13];
    return INTERCHANGE_FOOTER_SEGMENT + fieldDelimiter + "1" + fieldDelimiter + controlNumber;
  }

  /**
   * Creates a Functional Group Trailer segment (GE)
   * The control number from the GS segment is applied to the GE segment to ensure that the
   * Functional Group Footer is compatible with the Functional Group Header.
   * Segment structure:
   * <code>
   * GE*[number of transactions]*[control number]~
   * </code>
   */
  private String createFunctionalGroupTrailer() {
    String functionalGroupHeader = controlSegmentCache.get(FUNCTIONAL_GROUP_HEADER_SEGMENT);
    String fieldDelimiter = functionalGroupHeader.substring(2, 3);
    String[] functionalGroupFields = functionalGroupHeader
        .split(escapeDelimiter(fieldDelimiter));

    String controlNumber = functionalGroupFields[6];
    return FUNCTIONAL_GROUP_FOOTER_SEGMENT + fieldDelimiter + "1" + fieldDelimiter + controlNumber;
  }

    /**
     * Encloses a x12 transaction within a x12 control envelope. The envelope is formed as
     * ISA -> GS -> [data] -> GE -> IEA
     *
     * @param currentTransactionSegments The current transaction (list of segments)
     * @param x12LineSeparator The x12 line separator character
     * @return The enclosed transaction as a string
     */
  private String encloseTransaction(List<String> currentTransactionSegments,
      String x12LineSeparator) {

    return controlSegmentCache.get(INTERCHANGE_HEADER_SEGMENT) + x12LineSeparator +
        controlSegmentCache.get(FUNCTIONAL_GROUP_HEADER_SEGMENT) + x12LineSeparator +
        controlSegmentCache.get(TRANSACTION_HEADER_SEGMENT) + x12LineSeparator +
        String.join(x12LineSeparator, currentTransactionSegments) + x12LineSeparator +
        controlSegmentCache.get(TRANSACTION_FOOTER_SEGMENT) + x12LineSeparator +
        createFunctionalGroupTrailer() + x12LineSeparator +
        createInterchangeTrailer() + x12LineSeparator;
  }

}
