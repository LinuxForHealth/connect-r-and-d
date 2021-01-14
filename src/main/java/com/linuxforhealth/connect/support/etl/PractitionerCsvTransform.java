/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.linuxforhealth.connect.support.etl;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Address.AddressType;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;

import ca.uhn.fhir.context.FhirContext;

import java.util.UUID;

/**
 * A sample "trasform" class used with the
 * {@link com.linuxforhealth.connect.builder.EtlRouteBuilder} route. This class
 * transforms a {@PractitionerCsvFormat} record to the corresponding FHIR R4
 * Resources. Future implementations may implement a common interface/utilize
 * generic parameters for improved type safety.
 */
public class PractitionerCsvTransform {

    private FhirContext fhirContext = FhirContext.forR4();

    private static final String URN_UUID_PREFIX = "urn:uuid:";
    private static final String LOCATION_URL = "Location";
    private static final String ORGANIZATION_URL = "Organization";
    private static final String PRACTITIONER_URL = "Practitioner";
    private static final String PRACTITIONER_ROLE_URL = "PractitionerRole";

    private static final String NPI_SYSTEM_IDENTIFIER = "http://hl7.org/fhir/sid/us-npi";
    private static final String US_CORE_PROFILE_IDENTIFIER_PREFIX = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-";

    /**
     * Maps {@link PractitionerCsvFormat} organization data to a FHIR R4 {@link Organization} resource.
     *
     * @param csvFormat The source record
     * @return {@link Organization} record
     */
    private Organization mapOrganization(PractitionerCsvFormat csvFormat) {
        Organization org = new Organization();
        org.getMeta()
           .addProfile(US_CORE_PROFILE_IDENTIFIER_PREFIX + ORGANIZATION_URL.toLowerCase());

        org.addIdentifier()
           .setSystem(NPI_SYSTEM_IDENTIFIER)
           .setValue(csvFormat.getOrganizationNpi());

        org.setActive(true)
           .setName(csvFormat.getOrganizationName());

        return org;
    }

    /**
     * Maps {@link PractitionerCsvFormat} location data to a FHIR R4 {@link Location} resource.
     *
     * @param csvFormat The source record
     * @return {@link Location} record
     */
    private Location mapLocation(PractitionerCsvFormat csvFormat) {
        Location location = new Location();
        location.getMeta()
                .addProfile(US_CORE_PROFILE_IDENTIFIER_PREFIX + LOCATION_URL.toLowerCase());

        String locationName = csvFormat.getPractitionerLastName() + " " + " Practice Location";

        String addressLine2 = csvFormat.getPractitionerAddressLine2();
        if (addressLine2 != null && addressLine2.trim()
                                                .equalsIgnoreCase("")) {
            addressLine2 = null;
        }

        location.setStatus(LocationStatus.ACTIVE)
                .setName(locationName)
                .setMode(LocationMode.INSTANCE)
                .getAddress()
                .addLine(csvFormat.getPractitionerAddressLine1())
                .addLine(addressLine2)
                .setUse(AddressUse.WORK)
                .setType(AddressType.BOTH)
                .setCity(csvFormat.getPractitionerAddressCity())
                .setState(csvFormat.getPractitionerAddressState())
                .setPostalCode(csvFormat.getPractitionerAddressPostalCode())
                .setCountry(csvFormat.getPractitionerAddressCountry());
        return location;
    }

    /**
     * Maps {@link PractitionerCsvFormat} location data to a FHIR R4 {@link Practitioner} resource.
     *
     * @param csvFormat The source record
     * @return {@link Practitioner} record
     */
    private Practitioner mapPractitioner(PractitionerCsvFormat csvFormat) {
        Practitioner practitioner = new Practitioner();
        practitioner.getMeta()
                    .addProfile(US_CORE_PROFILE_IDENTIFIER_PREFIX + PRACTITIONER_URL.toLowerCase());

        practitioner.addIdentifier()
                    .setUse(IdentifierUse.OFFICIAL)
                    .setSystem(NPI_SYSTEM_IDENTIFIER)
                    .setValue(csvFormat.getPractitionerNpi());

        String middleName = csvFormat.getPractitionerMiddleName();
        if (middleName != null && middleName.trim()
                                            .equalsIgnoreCase("")) {
            middleName = null;
        }

        practitioner.addName()
                    .setUse(NameUse.OFFICIAL)
                    .setFamily(csvFormat.getPractitionerLastName())
                    .addGiven(csvFormat.getPractitionerFirstName())
                    .addGiven(middleName)
                    .addSuffix(csvFormat.getPractitionerSuffix());

        AdministrativeGender gender = null;

        switch (csvFormat.getPractitionerGender()) {
            case "male":
                gender = AdministrativeGender.MALE;
                break;
            case "female":
                gender = AdministrativeGender.FEMALE;
                break;
            default:
                gender = AdministrativeGender.NULL;
                break;
        }

        practitioner.setGender(gender)
                    .setBirthDate(csvFormat.getPractitionerBirthDate());

        return practitioner;
    }

    /**
     * Maps {@link PractitionerCsvFormat} location data to a FHIR R4 {@link PractitionerRole} resource.
     *
     * @param csvFormat The source record
     * @param orgUuid The organization uuid, used for resource linking in the transaction bundle.
     * @param practitionerUuid The practitioner uuid, used for resource linking in the transaction bundle.
     * @param locationUuid The location uuid, used for resource linking in the transaction bundle
     * @return {@link PractitionerRole} resource
     */
    private PractitionerRole mapPractitionerRole(PractitionerCsvFormat csvFormat,
                                                 UUID orgUuid,
                                                 UUID practitionerUuid,
                                                 UUID locationUuid) {
        PractitionerRole practitionerRole = new PractitionerRole();
        practitionerRole.getMeta()
                        .addProfile(US_CORE_PROFILE_IDENTIFIER_PREFIX + PRACTITIONER_ROLE_URL.toLowerCase());

        practitionerRole.setActive(true);

        practitionerRole.getPractitioner()
                        .setReference(URN_UUID_PREFIX + practitionerUuid);

        practitionerRole.getOrganization()
                        .setReference(URN_UUID_PREFIX + orgUuid);

        practitionerRole.addLocation()
                        .setReference(URN_UUID_PREFIX + locationUuid);

        practitionerRole.getTelecom()
                        .add(new ContactPoint()
                                .setUse(ContactPoint.ContactPointUse.WORK)
                                .setValue(csvFormat.getPractitionerPhone())
                                .setSystem(ContactPoint.ContactPointSystem.PHONE));

        String fax = csvFormat.getPractitionerFax();
        if (fax != null && !fax.trim().equalsIgnoreCase("")) {
            practitionerRole.getTelecom()
                            .add(new ContactPoint()
                                     .setUse(ContactPoint.ContactPointUse.WORK)
                                     .setValue(fax)
                                     .setSystem(ContactPoint.ContactPointSystem.FAX));
        }

        String email = csvFormat.getPractitionerEmail();
        if (email != null && !email.trim().equalsIgnoreCase("")) {
            practitionerRole.getTelecom()
                            .add(new ContactPoint()
                                         .setUse(ContactPoint.ContactPointUse.WORK)
                                         .setValue(email)
                                         .setSystem(ContactPoint.ContactPointSystem.EMAIL));
        }

        return practitionerRole;
    }

    /**
     * Maps a Practitioner CSV Record to a FHIR R4 Resource Bundle.
     *
     * @param csvFormat
     * @return
     */
    public String map(PractitionerCsvFormat csvFormat) {

        UUID orgUuid = UUID.randomUUID();
        UUID practitionerUuid = UUID.randomUUID();
        UUID practitionerRoleUuiD = UUID.randomUUID();
        UUID locationUuid = UUID.randomUUID();

        Bundle fhirBundle = new Bundle();
        fhirBundle.setType(BundleType.TRANSACTION);

        fhirBundle.addEntry()
                  .setFullUrl(URN_UUID_PREFIX + orgUuid.toString())
                  .setResource(mapOrganization(csvFormat))
                  .getRequest()
                  .setUrl(ORGANIZATION_URL)
                  .setMethod(HTTPVerb.POST);

        fhirBundle.addEntry()
                  .setFullUrl(URN_UUID_PREFIX + locationUuid.toString())
                  .setResource(mapLocation(csvFormat))
                  .getRequest()
                  .setUrl(LOCATION_URL)
                  .setMethod(HTTPVerb.POST);

        fhirBundle.addEntry()
                  .setFullUrl(URN_UUID_PREFIX + practitionerUuid.toString())
                  .setResource(mapPractitioner(csvFormat))
                  .getRequest()
                  .setUrl(PRACTITIONER_URL)
                  .setMethod(HTTPVerb.POST);

        fhirBundle.addEntry()
                  .setFullUrl(URN_UUID_PREFIX + practitionerRoleUuiD.toString())
                  .setResource(mapPractitionerRole(csvFormat, orgUuid, practitionerUuid, locationUuid))
                  .getRequest()
                  .setUrl(PRACTITIONER_ROLE_URL)
                  .setMethod(HTTPVerb.POST);

        return fhirContext.newJsonParser()
                          .encodeResourceToString(fhirBundle);
    }
}
