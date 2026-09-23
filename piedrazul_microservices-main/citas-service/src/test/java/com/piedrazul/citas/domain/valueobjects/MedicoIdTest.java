package com.piedrazul.citas.domain.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de MedicoId")
class MedicoIdTest {

    @Test
    @DisplayName("Debe crear un MedicoId válido con Long positivo")
    void testCrearValido() {
        MedicoId medicoId = new MedicoId(1L);

        assertNotNull(medicoId);
        assertEquals(1L, medicoId.value());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el valor es null")
    void testCrearConNullLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MedicoId(null);
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción si el valor es 0 o negativo")
    void testCrearConValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MedicoId(0L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new MedicoId(-1L);
        });
    }

    @Test
    @DisplayName("Debe crear con factory method of()")
    void testOf() {
        MedicoId medicoId = MedicoId.of(10L);

        assertNotNull(medicoId);
        assertEquals(10L, medicoId.value());
    }

    @Test
    @DisplayName("Dos MedicoId con el mismo valor deben ser iguales")
    void testEquals() {
        MedicoId id1 = MedicoId.of(5L);
        MedicoId id2 = MedicoId.of(5L);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}