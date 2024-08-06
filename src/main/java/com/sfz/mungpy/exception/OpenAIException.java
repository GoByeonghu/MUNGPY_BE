package com.sfz.mungpy.exception;

public class OpenAIException extends RuntimeException {

    public OpenAIException() {
        super();
    }

    public OpenAIException(String message) {
        super(message);
    }
}
