package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del patrón State de citas")
class EstadoCitaStateFactoryTest {

    @Test
    @DisplayName("PROGRAMADA permite las cuatro transiciones iniciales")
    void programadaPermiteTransicionesIniciales() {
        EstadoCitaState state = EstadoCitaStateFactory.of(EstadoCita.PROGRAMADA);
        java.time.LocalDateTime pasada = java.time.LocalDateTime.now().minusHours(1);

        assertTrue(state.puedeCancelar());
        assertTrue(state.puedeReagendarEnMismaCita());
        assertTrue(state.puedeMarcarComoAtendida(pasada));
        assertTrue(state.puedeMarcarComoNoAsistida(pasada));
        assertFalse(state.puedeReagendarCreandoNuevaCita());
    }

    @Test
    @DisplayName("ATENDIDA solo permite reagendar creando nueva cita")
    void atendidaPermiteSeguimiento() {
        EstadoCitaState state = EstadoCitaStateFactory.of(EstadoCita.ATENDIDA);

        assertFalse(state.puedeCancelar());
        assertFalse(state.puedeReagendarEnMismaCita());
        assertTrue(state.puedeReagendarCreandoNuevaCita());
        assertFalse(state.puedeMarcarComoAtendida(java.time.LocalDateTime.now().minusHours(1)));
    }

    @Test
    @DisplayName("REAGENDADA permite atendida, cancelada y no asistida tras la fecha")
    void reagendadaPermiteCierreDeCita() {
        EstadoCitaState state = EstadoCitaStateFactory.of(EstadoCita.REAGENDADA);
        java.time.LocalDateTime pasada = java.time.LocalDateTime.now().minusHours(2);
        java.time.LocalDateTime futura = java.time.LocalDateTime.now().plusDays(3);

        assertTrue(state.puedeCancelar());
        assertTrue(state.puedeMarcarComoAtendida(pasada));
        assertTrue(state.puedeMarcarComoNoAsistida(pasada));
        assertFalse(state.puedeMarcarComoAtendida(futura));
        assertFalse(state.puedeMarcarComoNoAsistida(futura));
        assertFalse(state.puedeReagendarEnMismaCita());
    }

    @Test
    @DisplayName("Estados finales no permiten cambios")
    void estadosFinales() {
        assertTrue(EstadoCitaStateFactory.of(EstadoCita.CANCELADA).esFinal());
        assertTrue(EstadoCitaStateFactory.of(EstadoCita.NO_ASISTIDA).esFinal());
        assertFalse(EstadoCitaStateFactory.of(EstadoCita.PROGRAMADA).esFinal());
    }
}
