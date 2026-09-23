package com.piedrazul.usuarios.application.exception;

public class DependenciaExternaException extends RuntimeException {
    public DependenciaExternaException(String message) {
        super(message);
    }
}