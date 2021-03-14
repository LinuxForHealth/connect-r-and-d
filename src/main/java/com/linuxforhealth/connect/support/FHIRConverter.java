/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverter {
  private static final HL7ToFHIRConverter CONVERTER = new HL7ToFHIRConverter();

  public String convert(String messageData) {
    try {
      String rawHL7Msg =
          new String(Base64.getDecoder().decode(messageData.getBytes(StandardCharsets.UTF_8)),
              StandardCharsets.UTF_8);
      String json = CONVERTER.convert(rawHL7Msg);
      Preconditions.checkState(StringUtils.isNotBlank(json), "No FHIR bundle resource generated");
      return json;
    } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
      throw new IllegalStateException("Failed to convert HL7 message to FHIR Resource", e);
    }
  }
}
