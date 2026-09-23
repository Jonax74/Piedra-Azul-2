package com.piedrazul.personas.application.exception;

public class PacienteYaRegistradoException extends RuntimeException {

    public PacienteYaRegistradoException(String message) {
        super(message);
    }
}