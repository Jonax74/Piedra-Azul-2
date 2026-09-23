package com.piedrazul.citas.domain.valueobjects;

public record MedicoId(Long value) {
    public MedicoId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("MedicoId inválido");
        }
    }

    public static MedicoId of(Long value) {
        return new MedicoId(value);
    }
}