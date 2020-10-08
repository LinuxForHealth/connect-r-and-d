package com.linuxforhealth.connect.support.x12;

public class X12ValidationException extends RuntimeException {

    public X12ValidationException() {}

    public X12ValidationException(String message) {super(message);}

    public X12ValidationException(Throwable ex) {super(ex);}

    public X12ValidationException(String message, Throwable ex) {super(message, ex);}

}
