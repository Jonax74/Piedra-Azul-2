package com.piedrazul.usuarios.application.exception;

public class PersonaNoEncontradaException extends RuntimeException {
    public PersonaNoEncontradaException(String message) {
        super(message);
    }
}