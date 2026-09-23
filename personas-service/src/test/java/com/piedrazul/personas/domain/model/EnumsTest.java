package com.piedrazul.personas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Enums")
class EnumsTest {

    @Test
    @DisplayName("Genero debe tener los valores correctos")
    void testGenero() {
        assertEquals(3, Genero.values().length);
        assertTrue(Genero.valueOf("HOMBRE") == Genero.HOMBRE);
        assertTrue(Genero.valueOf("MUJER") == Genero.MUJER);
        assertTrue(Genero.valueOf("OTRO") == Genero.OTRO);
    }

    @Test
    @DisplayName("EstadoMedico debe tener los valores correctos")
    void testEstadoMedico() {
        assertEquals(2, EstadoMedico.values().length);
        assertTrue(EstadoMedico.valueOf("ACTIVO") == EstadoMedico.ACTIVO);
        assertTrue(EstadoMedico.valueOf("INACTIVO") == EstadoMedico.INACTIVO);
    }

    @Test
    @DisplayName("TipoProfesional debe tener los valores correctos")
    void testTipoProfesional() {
        assertEquals(2, TipoProfesional.values().length);
        assertTrue(TipoProfesional.valueOf("MEDICO") == TipoProfesional.MEDICO);
        assertTrue(TipoProfesional.valueOf("TERAPISTA") == TipoProfesional.TERAPISTA);
    }

    @Test
    @DisplayName("EspecialidadMedica debe tener los valores correctos")
    void testEspecialidadMedica() {
        assertEquals(4, EspecialidadMedica.values().length);
        assertEquals(EspecialidadMedica.GENERAL, EspecialidadMedica.valueOf("GENERAL"));
        assertEquals(EspecialidadMedica.TERAPEUTA_NEURAL, EspecialidadMedica.valueOf("TERAPEUTA_NEURAL"));
        assertEquals(EspecialidadMedica.QUIROPRACTICO, EspecialidadMedica.valueOf("QUIROPRACTICO"));
        assertEquals(EspecialidadMedica.FISIOTERAPEUTA, EspecialidadMedica.valueOf("FISIOTERAPEUTA"));
    }
}