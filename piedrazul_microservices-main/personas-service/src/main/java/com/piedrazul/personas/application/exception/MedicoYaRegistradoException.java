package com.piedrazul.personas.application.exception;

public class MedicoYaRegistradoException extends RuntimeException {

    public MedicoYaRegistradoException(String message) {
        super(message);
    }
}