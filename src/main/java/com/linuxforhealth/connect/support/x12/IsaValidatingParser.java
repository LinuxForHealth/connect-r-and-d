package com.linuxforhealth.connect.support.x12;

import java.util.Arrays;
import java.util.List;

/**
 * Validates and parses a X12 ISA segment.
 */
public class IsaValidatingParser {

    public final static int ISA_SEGMENT_LENGTH = 106;

    private final String isaSegment;

    private String fieldDelimiter;

    private String repetitionCharacter;

    private String componentSeparator;

    private String lineSeparator;

    private final static String SEGMENT_NAME = "ISA";

    private final static List<String> META_CHARACTERS = Arrays.asList("*", "?", "|");

    private static final int[] fieldLengths = new int[]{
            2, // ISA 01
            10, // ISA 02
            2, // ISA 03
            10, // ISA 04
            2, // ISA 05
            15, // ISA 06
            2, // ISA 07
            15, // ISA 08
            6, // ISA 09
            4, // ISA 10
            1, // ISA 11
            5, // ISA 12
            9, // ISA 13
            1, // ISA 14
            1, // ISA 15
            1 // ISA 16
    };

    public String getIsaSegment() {
        return isaSegment;
    }

    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    public String getRepetitionCharacter() {
        return repetitionCharacter;
    }

    public String getComponentSeparator() {
        return componentSeparator;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Creates an instance of an ISA Segment Validator
     * @param isaSegment The data to validate
     * @throws NullPointerException if the ISA segment is null
     */
    public IsaValidatingParser(String isaSegment) {
        if (isaSegment == null) {
            throw new NullPointerException("ISA segment is null");
        }

        this.isaSegment = isaSegment;
        init();
    }

    /**
     * Validates the total length of the ISA Segment
     * @throws X12ValidationException if the provided segment length is not correct
     */
    private void validateSegmentLength() {
        if (isaSegment.length() != ISA_SEGMENT_LENGTH) {
            String message = "Expected Segment Length: " + ISA_SEGMENT_LENGTH + " Actual Segment Length: " + isaSegment.length();
            throw new X12ValidationException(message);
        }
    }

    /**
     * Validates the segment name
     * @throws X12ValidationException if the segment name is incorrect
     */
    private void validateSegmentName() {
        String segmentName = isaSegment.substring(0, 3);
        if (!SEGMENT_NAME.equalsIgnoreCase(segmentName)) {
            String message = "Expected Segment Name: " + SEGMENT_NAME + " Actual Segment Name: " + segmentName;
            throw new X12ValidationException(message);
        }
    }

    /**
     * Validates the absolute field lengths in the ISA segment
     * @throws if the expected number of fields or field length is incorrect
     */
    private void validateFieldLengths()  {
        String splitCharacter = fieldDelimiter;
        if (META_CHARACTERS.contains(fieldDelimiter)) {
            splitCharacter = "\\" + fieldDelimiter;
        }

        // exclude the segment name and first delimiter as well as the final delimiter
        String segmentData = isaSegment.substring(4, isaSegment.length() - 1);
        String[] isaFields = segmentData.split(splitCharacter);

        if (isaFields.length != fieldLengths.length) {
            String message = "Expected Number of Fields: " + fieldLengths.length + " Actual Fields: " + isaFields.length;
            throw new X12ValidationException(message);
        }

        for (int i=0; i<isaFields.length; i++) {
            int isaFieldLength = isaFields[i].length();
            if (isaFieldLength!= fieldLengths[i]) {
                String message = "ISA Field " + i + " Expected Length:" + fieldLengths[i] + " Actual Length:" + isaFieldLength;
                throw new X12ValidationException(message);
            }
        }
    }

    /**
     * Validates and parses the ISA segment
     * @throws X12ValidationException if a validation error is found
     */
    private void init()  {
        validateSegmentLength();
        validateSegmentName();
        fieldDelimiter = isaSegment.substring(3, 4);
        validateFieldLengths();
        repetitionCharacter = isaSegment.substring(82, 83);
        componentSeparator = isaSegment.substring(104, 105);
        lineSeparator = isaSegment.substring(105, 106);
    }
}
