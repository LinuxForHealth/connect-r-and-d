/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.linuxforhealth.connect.support.etl;

import java.text.SimpleDateFormat;

import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests transformations defined in {@link PractitionerCsvTransform}.
 * Due to verbose/comprehensive nature of the FHIR format, tests are grouped
 * by data type/domain rather than verified in a single test.
 */
public class PractitionerCsvTransformTest {

  private PractitionerCsvFormat sourceRecord;
  private Bundle targetBundle;

  /**
   * Configures a sample CSV fixture and supporting transformation components.
   *
   * @throws Exception
   */
  @BeforeEach
  void beforeEach() throws Exception {
    sourceRecord = new PractitionerCsvFormat();
    sourceRecord.setPractitionerNpi("1912301953");
    sourceRecord.setPractitionerFirstName("All");
    sourceRecord.setPractitionerMiddleName("");
    sourceRecord.setPractitionerLastName("Good");
    sourceRecord.setPractitionerSuffix("MD");
    sourceRecord.setPractitionerGender("female");
    sourceRecord
        .setPractitionerBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1971-11-07"));
    sourceRecord.setPractitionerAddressLine1("1400 Anyhoo Lane");
    sourceRecord.setPractitionerAddressLine2("Suite 100");
    sourceRecord.setPractitionerAddressCity("Spartanburg");
    sourceRecord.setPractitionerAddressState("SC");
    sourceRecord.setPractitionerAddressPostalCode("90210");
    sourceRecord.setPractitionerAddressCountry("US");
    sourceRecord.setPractitionerPhone("5551114444");
    sourceRecord.setPractitionerEmail("allgood@health.com");
    sourceRecord.setPractitionerFax("5551113333");
    sourceRecord.setOrganizationName("Allgood Practice Group");
    sourceRecord.setOrganizationNpi("1144221847");

    PractitionerCsvTransform practitionerCsvTransform = new PractitionerCsvTransform();

    targetBundle = FhirContext.forR4().newJsonParser()
        .parseResource(Bundle.class, practitionerCsvTransform.map(sourceRecord));

  }

  /**
   * Returns the first bundle entry for a specified resource type.
   * @param fhirBundle The {@link Bundle} to search
   * @param resourceType The resource type name / lookup key
   * @return matching {@link BundleEntryComponent}, otherwise raises an exception
   *
   */
  private BundleEntryComponent findFirstBundleEntry(Bundle fhirBundle, String resourceType) {
    Optional<BundleEntryComponent> firstComponent = fhirBundle.getEntry()
                                                  .stream()
                                                  .filter(b -> b.getRequest()
                                                      .getUrl()
                                                      .equalsIgnoreCase(resourceType))
                                                  .findFirst();
    return firstComponent.get();
    }

  /**
   * Validates that the metadata components of the target/generated FHIR resources.
   * Validated fields include:
   * <ul>
   *   <li>request</li>
   *   <li>fullUrl</li>
   * </ul>
   *
   */
  @ParameterizedTest
  @ValueSource(strings = {"Organization", "Location", "Practitioner", "PractitionerRole"})
  void testTargetMetadata(String resourceType) {
    BundleEntryComponent targetEntry = findFirstBundleEntry(targetBundle, resourceType);
    Assertions.assertNotNull(targetEntry);
    Assertions.assertEquals("POST", targetEntry.getRequest().getMethod().toString());
    Assertions.assertEquals(resourceType, targetEntry.getRequest().getUrl());
    Assertions.assertNotNull(targetEntry.getFullUrl());

    Meta meta = targetEntry.getResource().getMeta();
    String coreProfile = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-";
    Assertions.assertEquals(coreProfile + resourceType.toLowerCase(),
        meta.getProfile().get(0).asStringValue());

    Assertions.assertNotNull(targetEntry.getFullUrl());
  }

  /**
   * Validates the {@link Organization} resource mapping
   */
  @Test
  void testTargetOrganization() {
    BundleEntryComponent targetEntry = findFirstBundleEntry(targetBundle, "Organization");
    Assertions.assertNotNull(targetEntry);

    Organization targetOrg = (Organization) targetEntry.getResource();
    Assertions.assertTrue(targetOrg.getActive());
    Assertions.assertEquals(targetOrg.getName(), sourceRecord.getOrganizationName());

    Assertions.assertEquals("http://hl7.org/fhir/sid/us-npi",
        targetOrg.getIdentifier().get(0).getSystem());

  }

  /**
   * Validates {@link Location} resource mapping
   */
  @Test
  void testTargetLocation() {
    BundleEntryComponent targetEntry = findFirstBundleEntry(targetBundle, "Location");
    Assertions.assertNotNull(targetEntry);

    Location targetLocation = (Location) targetEntry.getResource();
    Assertions.assertEquals("ACTIVE", targetLocation.getStatus().toString());
    Assertions.assertNotNull(targetLocation.getName());
    Assertions.assertEquals("INSTANCE", targetLocation.getMode().toString());

    Address targetAddress = targetLocation.getAddress();
    Assertions.assertEquals("WORK", targetAddress.getUse().toString());
    Assertions.assertEquals("BOTH", targetAddress.getType().toString());
    Assertions.assertEquals(targetAddress.getLine().get(0).toString(), sourceRecord.getPractitionerAddressLine1());
    Assertions.assertEquals(targetAddress.getLine().get(1).toString(), sourceRecord.getPractitionerAddressLine2());
    Assertions.assertEquals(targetAddress.getCity(), sourceRecord.getPractitionerAddressCity());
    Assertions.assertEquals(targetAddress.getState(), sourceRecord.getPractitionerAddressState());
    Assertions.assertEquals(targetAddress.getPostalCode(), sourceRecord.getPractitionerAddressPostalCode());
    Assertions.assertEquals(targetAddress.getCountry(), sourceRecord.getPractitionerAddressCountry());
  }

  /**
   * Validates {@link Practitioner} resource mapping
   */
  @Test
  void testPractitionerResourceMapping() {
    BundleEntryComponent targetEntry = findFirstBundleEntry(targetBundle, "Practitioner");
    Assertions.assertNotNull(targetEntry);

    Practitioner targetPractitioner = (Practitioner) targetEntry.getResource();
    Identifier targetId = targetPractitioner.getIdentifier().get(0);

    Assertions.assertEquals("OFFICIAL", targetId.getUse().toString());
    Assertions.assertEquals("http://hl7.org/fhir/sid/us-npi", targetId.getSystem());
    Assertions.assertEquals(targetId.getValue(), sourceRecord.getPractitionerNpi());

    HumanName targetName = targetPractitioner.getName().get(0);
    Assertions.assertEquals("OFFICIAL", targetName.getUse().toString());
    Assertions.assertEquals(targetName.getFamily(), sourceRecord.getPractitionerLastName());
    Assertions.assertEquals(targetName.getGiven().get(0).toString(), sourceRecord.getPractitionerFirstName());
    Assertions.assertEquals(1, targetName.getGiven().size());
    Assertions.assertEquals(1, targetName.getSuffix().size());
    Assertions.assertEquals(targetName.getSuffix().get(0).toString(), sourceRecord.getPractitionerSuffix());

    Assertions.assertEquals("FEMALE", targetPractitioner.getGender().toString());
    Assertions.assertEquals(targetPractitioner.getBirthDate(), sourceRecord.getPractitionerBirthDate());
  }

  /**
   * Validates {@link PractitionerRole} resource mapping
   */
  @Test
  void testPractitionerRoleResourceMapping() {
    BundleEntryComponent targetEntry = findFirstBundleEntry(targetBundle, "PractitionerRole");
    Assertions.assertNotNull(targetEntry);

    PractitionerRole targetRole = (PractitionerRole) targetEntry.getResource();
    Assertions.assertTrue(targetRole.getActive());

    Assertions.assertNotNull(targetRole.getPractitioner());
    Assertions.assertNotNull(targetRole.getOrganization());
    Assertions.assertNotNull(targetRole.getLocation());

    String phone = null;
    String email = null;
    String fax = null;

    for (ContactPoint cp: targetRole.getTelecom()) {
      Assertions.assertEquals("WORK", cp.getUse().toString());
      String system = cp.getSystem().toString();

      switch (system) {
        case "PHONE":
          phone = cp.getValue();
          break;
        case "EMAIL":
          email = cp.getValue();
          break;
        case "FAX":
          fax = cp.getValue();
      }
    }
    Assertions.assertEquals(phone, sourceRecord.getPractitionerPhone());
    Assertions.assertEquals(email, sourceRecord.getPractitionerEmail());
    Assertions.assertEquals(fax, sourceRecord.getPractitionerFax());
  }
}
