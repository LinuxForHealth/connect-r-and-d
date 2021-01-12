package com.linuxforhealth.connect.support.etl;

public class PractitionerCsvTransform {

    public String map(PractitionerCsvFormat csvFormat) {
        return csvFormat.getPractitionerNpi() + " " + csvFormat.getPractitionerLastName();
    }
}
