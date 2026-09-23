package com.piedrazul.citas.domain.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de CitaId")
class CitaIdTest {

    @Test
    @DisplayName("Debe crear un CitaId válido con UUID")
    void testCrearConUUID() {
        UUID uuid = UUID.randomUUID();
        CitaId citaId = new CitaId(uuid);

        assertNotNull(citaId);
        assertEquals(uuid, citaId.value());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el UUID es null")
    void testCrearConNullLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CitaId(null);
        });
    }

    @Test
    @DisplayName("Debe generar un UUID automáticamente con generate()")
    void testGenerate() {
        CitaId citaId = CitaId.generate();

        assertNotNull(citaId);
        assertNotNull(citaId.value());
    }

    @Test
    @DisplayName("Debe crear desde String con fromString()")
    void testFromString() {
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        CitaId citaId = CitaId.fromString(uuidString);

        assertNotNull(citaId);
        assertEquals(uuidString, citaId.toString());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el String no es un UUID válido")
    void testFromStringInvalido() {
        assertThrows(IllegalArgumentException.class, () -> {
            CitaId.fromString("no-es-un-uuid");
        });
    }

    @Test
    @DisplayName("Dos CitaId con el mismo UUID deben ser iguales")
    void testEquals() {
        UUID uuid = UUID.randomUUID();
        CitaId citaId1 = new CitaId(uuid);
        CitaId citaId2 = new CitaId(uuid);

        assertEquals(citaId1, citaId2);
        assertEquals(citaId1.hashCode(), citaId2.hashCode());
    }
}