package com.piedrazul.citas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de EstadoCita")
class EstadoCitaTest {

    @Test
    @DisplayName("Debe contener los cinco estados del ciclo de vida")
    void testCantidadEstados() {
        assertEquals(5, EstadoCita.values().length);
    }

    @Test
    @DisplayName("Cada estado debe tener su descripción")
    void testDescripciones() {
        assertEquals("Programada", EstadoCita.PROGRAMADA.getDescripcion());
        assertEquals("Atendida", EstadoCita.ATENDIDA.getDescripcion());
        assertEquals("Cancelada", EstadoCita.CANCELADA.getDescripcion());
        assertEquals("No Asistida", EstadoCita.NO_ASISTIDA.getDescripcion());
        assertEquals("Reagendada", EstadoCita.REAGENDADA.getDescripcion());
    }

    @Test
    @DisplayName("Debe mapear CONFIRMADA legada a PROGRAMADA")
    void testFromPersistedConfirmada() {
        assertEquals(EstadoCita.PROGRAMADA, EstadoCita.fromPersisted("CONFIRMADA"));
    }

    @Test
    @DisplayName("Debe permitir cancelarse en PROGRAMADA y REAGENDADA")
    void testPuedeCancelarse() {
        assertTrue(EstadoCita.PROGRAMADA.puedeCancelarse());
        assertTrue(EstadoCita.REAGENDADA.puedeCancelarse());
        assertFalse(EstadoCita.ATENDIDA.puedeCancelarse());
        assertFalse(EstadoCita.CANCELADA.puedeCancelarse());
        assertFalse(EstadoCita.NO_ASISTIDA.puedeCancelarse());
    }

    @Test
    @DisplayName("Debe permitir reagendarse en la misma cita solo desde PROGRAMADA")
    void testPuedeReagendarseEnMismaCita() {
        assertTrue(EstadoCita.PROGRAMADA.puedeReagendarseEnMismaCita());
        assertFalse(EstadoCita.REAGENDADA.puedeReagendarseEnMismaCita());
        assertFalse(EstadoCita.ATENDIDA.puedeReagendarseEnMismaCita());
    }

    @Test
    @DisplayName("Debe permitir crear nueva cita al reagendar desde ATENDIDA")
    void testPuedeReagendarseCreandoNuevaCita() {
        assertTrue(EstadoCita.ATENDIDA.puedeReagendarseCreandoNuevaCita());
        assertFalse(EstadoCita.PROGRAMADA.puedeReagendarseCreandoNuevaCita());
    }

    @Test
    @DisplayName("Debe permitir marcar asistencia solo si la fecha y hora ya pasaron")
    void testPuedeMarcarseAsistencia() {
        LocalDateTime pasada = LocalDateTime.now().minusHours(1);
        LocalDateTime futura = LocalDateTime.now().plusDays(1);

        assertTrue(EstadoCita.PROGRAMADA.puedeMarcarseComoAtendida(pasada));
        assertTrue(EstadoCita.REAGENDADA.puedeMarcarseComoAtendida(pasada));
        assertFalse(EstadoCita.PROGRAMADA.puedeMarcarseComoAtendida(futura));
        assertFalse(EstadoCita.REAGENDADA.puedeMarcarseComoAtendida(futura));
        assertTrue(EstadoCita.PROGRAMADA.puedeMarcarseComoNoAsistida(pasada));
        assertTrue(EstadoCita.REAGENDADA.puedeMarcarseComoNoAsistida(pasada));
        assertFalse(EstadoCita.PROGRAMADA.puedeMarcarseComoNoAsistida(futura));
        assertFalse(EstadoCita.REAGENDADA.puedeMarcarseComoNoAsistida(futura));
        assertFalse(EstadoCita.CANCELADA.puedeMarcarseComoAtendida(pasada));
    }
}
