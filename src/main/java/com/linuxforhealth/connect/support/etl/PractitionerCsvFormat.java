package com.linuxforhealth.connect.support.etl;

import java.util.Date;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

/**
 * A sample "format" class used with the {@link com.linuxforhealth.connect.builder.ETLRouteBuilder} route.
 * This class uses Bindy annotations to support parsing a CSV based record format.
 */
@CsvRecord(separator = ",", quoting = true, skipFirstLine = true, generateHeaderColumns = true)
public class PractitionerCsvFormat {

    @DataField(pos = 1, columnName = "practitioner_npi")
    private String practitionerNpi;

    @DataField(pos = 2, columnName = "practitioner_first_name")
    private String practitionerFirstName;

    @DataField(pos = 3, columnName = "practitioner_middle_name")
    private String practitionerMiddleName;

    @DataField(pos = 4, columnName = "practitioner_last_name")
    private String practitionerLastName;

    @DataField(pos = 5, columnName = "practitioner_suffix")
    private String practitionerSuffix;

    @DataField(pos = 6, columnName = "practitioner_gender", pattern = "male|female")
    private String practitionerGender;

    @DataField(pos = 7, columnName = "practitioner_birth_date", pattern = "yyyy-MM-dd")
    private Date practitionerBirthDate;

    @DataField(pos = 8, columnName = "practitioner_address_line_1")
    private String practitionerAddressLine1;

    @DataField(pos = 9, columnName = "practitioner_address_line_2")
    private String practitionerAddressLine2;

    @DataField(pos = 10, columnName = "practitioner_address_city")
    private String practitionerAddressCity;

    @DataField(pos = 11, columnName = "practitioner_address_state")
    private String practitionerAddressState;

    @DataField(pos = 12, columnName = "practitioner_address_postal_code")
    private String practitionerAddressPostalCode;

    @DataField(pos = 13, columnName = "practitioner_address_country")
    private String practitionerAddressCountry;

    @DataField(pos = 14, columnName = "practitioner_phone")
    private String practitionerPhone;

    @DataField(pos = 15, columnName = "practitioner_email")
    private String practitionerEmail;

    @DataField(pos = 16, columnName = "practitioner_fax")
    private String practitionerFax;

    @DataField(pos = 17, columnName = "organization_name")
    private String organizationName;

    @DataField(pos = 18, columnName = "organization_npi")
    private String organizationNpi;

    public String getPractitionerNpi() {
        return practitionerNpi;
    }

    public void setPractitionerNpi(String practitionerNpi) {
        this.practitionerNpi = practitionerNpi;
    }

    public String getPractitionerFirstName() {
        return practitionerFirstName;
    }

    public void setPractitionerFirstName(String practitionerFirstName) {
        this.practitionerFirstName = practitionerFirstName;
    }

    public String getPractitionerMiddleName() {
        return practitionerMiddleName;
    }

    public void setPractitionerMiddleName(String practitionerMiddleName) {
        this.practitionerMiddleName = practitionerMiddleName;
    }

    public String getPractitionerLastName() {
        return practitionerLastName;
    }

    public void setPractitionerLastName(String practitionerLastName) {
        this.practitionerLastName = practitionerLastName;
    }

    public String getPractitionerSuffix() {
        return practitionerSuffix;
    }

    public void setPractitionerSuffix(String practitionerSuffix) {
        this.practitionerSuffix = practitionerSuffix;
    }

    public String getPractitionerGender() {
        return practitionerGender;
    }

    public void setPractitionerGender(String practitionerGender) {
        this.practitionerGender = practitionerGender;
    }

    public Date getPractitionerBirthDate() {
        return practitionerBirthDate;
    }

    public void setPractitionerBirthDate(Date practitionerBirthDate) {
        this.practitionerBirthDate = practitionerBirthDate;
    }

    public String getPractitionerAddressLine1() {
        return practitionerAddressLine1;
    }

    public void setPractitionerAddressLine1(String practitionerAddressLine1) {
        this.practitionerAddressLine1 = practitionerAddressLine1;
    }

    public String getPractitionerAddressLine2() {
        return practitionerAddressLine2;
    }

    public void setPractitionerAddressLine2(String practitionerAddressLine2) {
        this.practitionerAddressLine2 = practitionerAddressLine2;
    }

    public String getPractitionerAddressCity() {
        return practitionerAddressCity;
    }

    public void setPractitionerAddressCity(String practitionerAddressCity) {
        this.practitionerAddressCity = practitionerAddressCity;
    }

    public String getPractitionerAddressState() {
        return practitionerAddressState;
    }

    public void setPractitionerAddressState(String practitionerAddressState) {
        this.practitionerAddressState = practitionerAddressState;
    }

    public String getPractitionerAddressPostalCode() {
        return practitionerAddressPostalCode;
    }

    public void setPractitionerAddressPostalCode(String practitionerAddressPostalCode) {
        this.practitionerAddressPostalCode = practitionerAddressPostalCode;
    }

    public String getPractitionerAddressCountry() {
        return practitionerAddressCountry;
    }

    public void setPractitionerAddressCountry(String practitionerAddressCountry) {
        this.practitionerAddressCountry = practitionerAddressCountry;
    }

    public String getPractitionerPhone() {
        return practitionerPhone;
    }

    public void setPractitionerPhone(String practitionerPhone) {
        this.practitionerPhone = practitionerPhone;
    }

    public String getPractitionerEmail() {
        return practitionerEmail;
    }

    public void setPractitionerEmail(String practitionerEmail) {
        this.practitionerEmail = practitionerEmail;
    }

    public String getPractitionerFax() {
        return practitionerFax;
    }

    public void setPractitionerFax(String practitionerFax) {
        this.practitionerFax = practitionerFax;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationNpi() {
        return organizationNpi;
    }

    public void setOrganizationNpi(String organizationNpi) {
        this.organizationNpi = organizationNpi;
    }
}
