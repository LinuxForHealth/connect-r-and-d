package com.linuxforhealth.connect.support.etl;

import java.util.Date;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

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

     @DataField(pos = 6, columnName = "practitioner_gender")
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

     public String getPractitionerFirstName() {
         return practitionerFirstName;
     }

     public String getPractitionerMiddleName() {
         return practitionerMiddleName;
     }

     public String getPractitionerLastName() {
         return practitionerLastName;
     }

     public String getPractitionerSuffix() {
         return practitionerSuffix;
     }

     public String getPractitionerGender() {
         return practitionerGender;
     }

     public Date getPractitionerBirthDate() {
         return practitionerBirthDate;
     }

     public String getPractitionerAddressLine1() {
         return practitionerAddressLine1;
     }

     public String getPractitionerAddressLine2() {
         return practitionerAddressLine2;
     }

     public String getPractitionerAddressCity() {
         return practitionerAddressCity;
     }

     public String getPractitionerAddressState() {
         return practitionerAddressState;
     }

     public String getPractitionerAddressPostalCode() {
         return practitionerAddressPostalCode;
     }

     public String getPractitionerAddressCountry() {
         return practitionerAddressCountry;
     }

     public String getPractitionerPhone() {
         return practitionerPhone;
     }

     public String getPractitionerEmail() {
         return practitionerEmail;
     }

     public String getPractitionerFax() {
         return practitionerFax;
     }

     public String getOrganizationName() {
         return organizationName;
     }

     public String getOrganizationNpi() {
         return organizationNpi;
     }
}
