package com.piedrazul.personas.application.exception;

public class PersonaNoEncontradaException extends RuntimeException {

    public PersonaNoEncontradaException(String message) {
        super(message);
    }
}