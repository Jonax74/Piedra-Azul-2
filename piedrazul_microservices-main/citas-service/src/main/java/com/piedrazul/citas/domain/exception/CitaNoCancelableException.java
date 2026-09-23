package com.piedrazul.citas.domain.exception;

public class CitaNoCancelableException extends RuntimeException {
    public CitaNoCancelableException(String message) {
        super(message);
    }
}