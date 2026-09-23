package com.piedrazul.citas.domain.valueobjects;

public record UsuarioId(Long value) {
    public UsuarioId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UsuarioId inválido");
        }
    }

    public static UsuarioId of(Long value) {
        return new UsuarioId(value);
    }
}