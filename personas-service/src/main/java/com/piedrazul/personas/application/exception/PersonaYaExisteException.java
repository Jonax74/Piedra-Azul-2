package com.piedrazul.personas.application.exception;

public class PersonaYaExisteException extends RuntimeException {

    public PersonaYaExisteException(String message) {
        super(message);
    }
}