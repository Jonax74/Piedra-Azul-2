package com.piedrazul.personas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Medico")
class MedicoTest {

    @Test
    @DisplayName("Debería crear un médico correctamente")
    void testCrearMedico() {
        Medico medico = Medico.crear(1L, TipoProfesional.MEDICO);

        assertNotNull(medico);
        assertEquals(1L, medico.getPersonaId());
        assertEquals(TipoProfesional.MEDICO, medico.getTipoProfesional());
        assertEquals(EstadoMedico.ACTIVO, medico.getEstado());
        assertTrue(medico.estaActivo());
    }

    @Test
    @DisplayName("Debería lanzar excepción si personaId es null")
    void testPersonaIdNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Medico.crear(null, TipoProfesional.MEDICO);
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si personaId es 0")
    void testPersonaIdCero() {
        assertThrows(IllegalArgumentException.class, () -> {
            Medico.crear(0L, TipoProfesional.MEDICO);
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si tipo profesional es nulo")
    void testTipoProfesionalNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            Medico.crear(1L, null);
        });
    }

    @Test
    @DisplayName("Debería cambiar el estado del médico")
    void testCambiarEstado() {
        Medico medico = Medico.crear(1L, TipoProfesional.MEDICO);
        medico.cambiarEstado(EstadoMedico.INACTIVO);

        assertEquals(EstadoMedico.INACTIVO, medico.getEstado());
        assertFalse(medico.estaActivo());
    }

    @Test
    @DisplayName("Debería crear médico con especialidades")
    void testCrearMedicoConEspecialidades() {
        Medico medico = Medico.crear(
                1L,
                TipoProfesional.MEDICO,
                Set.of(EspecialidadMedica.GENERAL, EspecialidadMedica.FISIOTERAPEUTA)
        );

        assertEquals(2, medico.getEspecialidades().size());
        assertTrue(medico.tieneEspecialidad(EspecialidadMedica.GENERAL));
        assertTrue(medico.tieneEspecialidad(EspecialidadMedica.FISIOTERAPEUTA));
    }

    @Test
    @DisplayName("Debería rechazar médico sin especialidades")
    void testEspecialidadesVacias() {
        assertThrows(IllegalArgumentException.class, () ->
                Medico.crear(1L, TipoProfesional.MEDICO, Set.of())
        );
    }
}