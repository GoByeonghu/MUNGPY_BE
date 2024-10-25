package com.sfz.mungpy.exception;

public class ShelterNotFoundException extends RuntimeException {

    public ShelterNotFoundException() {
        super();
    }

    public ShelterNotFoundException(String message) {
        super(message);
    }
}
