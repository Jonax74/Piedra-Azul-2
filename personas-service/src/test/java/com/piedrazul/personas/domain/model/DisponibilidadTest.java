package com.piedrazul.personas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Disponibilidad")
class DisponibilidadTest {

    @Test
    @DisplayName("Debería crear una disponibilidad correctamente")
    void testCrearDisponibilidad() {
        Disponibilidad disponibilidad = new Disponibilidad(
                1L, "LUNES", LocalTime.of(8, 0), LocalTime.of(17, 0), 30
        );

        assertNotNull(disponibilidad);
        assertEquals(1L, disponibilidad.getMedicoId());
        assertEquals("LUNES", disponibilidad.getDiaSemana());
        assertEquals(LocalTime.of(8, 0), disponibilidad.getHoraInicio());
        assertEquals(LocalTime.of(17, 0), disponibilidad.getHoraFin());
        assertEquals(30, disponibilidad.getIntervaloMinutos());
    }

    @Test
    @DisplayName("Debería poder modificar los atributos")
    void testSetters() {
        Disponibilidad disponibilidad = new Disponibilidad();
        disponibilidad.setId(1L);
        disponibilidad.setMedicoId(2L);
        disponibilidad.setDiaSemana("MARTES");
        disponibilidad.setHoraInicio(LocalTime.of(9, 0));
        disponibilidad.setHoraFin(LocalTime.of(18, 0));
        disponibilidad.setIntervaloMinutos(45);

        assertEquals(1L, disponibilidad.getId());
        assertEquals(2L, disponibilidad.getMedicoId());
        assertEquals("MARTES", disponibilidad.getDiaSemana());
        assertEquals(LocalTime.of(9, 0), disponibilidad.getHoraInicio());
        assertEquals(LocalTime.of(18, 0), disponibilidad.getHoraFin());
        assertEquals(45, disponibilidad.getIntervaloMinutos());
    }
}