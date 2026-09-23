package com.piedrazul.citas.domain.valueobjects;

public record PacienteId(Long value) {
    public PacienteId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("PacienteId inválido");
        }
    }

    public static PacienteId of(Long value) {
        return new PacienteId(value);
    }
}