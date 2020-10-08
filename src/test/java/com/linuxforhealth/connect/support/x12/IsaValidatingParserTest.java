package com.linuxforhealth.connect.support.x12;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link IsaValidatingParser}
 */
class IsaValidatingParserTest {
    private final static String VALID_ISA_SEGMENT = "ISA*00*          *00*          *ZZ*890069730      *ZZ*154663145      *200929*1705*|*00501*000000001*0*T*:~";

    /**
     * Tests ISA validation and parsing when the ISA segment is valid.
     */
    @Test
    void testIsaValidator() {
        IsaValidatingParser parser = null;
        try {
            parser = new IsaValidatingParser(VALID_ISA_SEGMENT);
        } catch (X12ValidationException ex) {
            Assertions.fail("Unexpected validation error", ex);
        }

        Assertions.assertEquals(VALID_ISA_SEGMENT, parser.getIsaSegment());
        Assertions.assertEquals("*", parser.getFieldDelimiter());
        Assertions.assertEquals("|", parser.getRepetitionCharacter());
        Assertions.assertEquals(":", parser.getComponentSeparator());
        Assertions.assertEquals("~", parser.getLineSeparator());
    }

    /**
     * Tests ISA validation and parsing when the ISA segment length is invalid.
     */
    @Test
    void testIsaValidatorInvalidLength() {
        String isaSegment = VALID_ISA_SEGMENT.substring(0, 25);
        Assertions.assertThrows(X12ValidationException.class, () -> {
            new IsaValidatingParser(isaSegment);
        });
    }

    /**
     * Tests ISA validation and parsing when an ISA segment field length is invalid.
     */
    @Test
    void testIsaValidatorInvalidFieldLength() {
        String isaSegment = VALID_ISA_SEGMENT.replace("890069730      ", "890069730890069730890069730890069730890069730");
        Assertions.assertThrows(X12ValidationException.class, () -> {
            new IsaValidatingParser(isaSegment);
        });
    }

    /**
     * Tests ISA validation and parsing when the ISA segment name is invalid.
     */
    @Test
    void testIsaValidatorInvalidName() {
        String isaSegment = VALID_ISA_SEGMENT.replace("ISA", "FOO");
        Assertions.assertThrows(X12ValidationException.class, () -> {
            new IsaValidatingParser(isaSegment);
        });
    }

}
