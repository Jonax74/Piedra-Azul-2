// domain/exception/MedicoNoDisponibleException.java
package com.piedrazul.citas.domain.exception;

public class MedicoNoDisponibleException extends RuntimeException {
    public MedicoNoDisponibleException(String message) {
        super(message);
    }
}