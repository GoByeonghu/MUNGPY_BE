package com.sfz.mungpy.exception;

public class DogNotFoundException extends RuntimeException {

    public DogNotFoundException() {
        super();
    }

    public DogNotFoundException(String message) {
        super(message);
    }
}
