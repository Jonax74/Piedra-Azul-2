package com.piedrazul.citas.domain.exception;

public class PacienteNoExisteException extends RuntimeException {
    public PacienteNoExisteException(String message) {
        super(message);
    }
}