package com.piedrazul.personas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Paciente")
class PacienteTest {

    @Test
    @DisplayName("Debería crear un paciente correctamente")
    void testCrearPaciente() {
        Paciente paciente = new Paciente(1L);

        assertNotNull(paciente);
        assertEquals(1L, paciente.getPersonaId());
    }

    @Test
    @DisplayName("Debería lanzar excepción si personaId es null")
    void testPersonaIdNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Paciente(null);
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si personaId es 0")
    void testPersonaIdCero() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Paciente(0L);
        });
    }

    @Test
    @DisplayName("Debería poder modificar el personaId")
    void testSetPersonaId() {
        Paciente paciente = new Paciente(1L);
        paciente.setPersonaId(2L);

        assertEquals(2L, paciente.getPersonaId());
    }

    @Test
    @DisplayName("Dos pacientes con el mismo personaId deben ser iguales")
    void testEquals() {
        Paciente p1 = new Paciente(1L);
        Paciente p2 = new Paciente(1L);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}