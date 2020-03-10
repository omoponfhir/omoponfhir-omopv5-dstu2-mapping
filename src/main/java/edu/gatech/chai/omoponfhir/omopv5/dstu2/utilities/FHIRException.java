package edu.gatech.chai.omoponfhir.omopv5.dstu2.utilities;

public class FHIRException extends RuntimeException {

    // Note that the 4-argument constructor has been removed as it is not JDK6 compatible

    public FHIRException() {
        super();
    }

    public FHIRException(String message, Throwable cause) {
        super(message, cause);
    }

    public FHIRException(String message) {
        super(message);
    }

    public FHIRException(Throwable cause) {
        super(cause);
    }

}