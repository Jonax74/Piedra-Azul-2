package com.piedrazul.citas.domain.valueobjects;

import java.util.UUID;

public record CitaId(UUID value) {
    public CitaId {
        if (value == null) {
            throw new IllegalArgumentException("CitaId no puede ser null");
        }
    }

    public static CitaId generate() {
        return new CitaId(UUID.randomUUID());
    }

    public static CitaId fromString(String value) {
        return new CitaId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}