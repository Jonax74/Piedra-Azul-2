package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.MedicoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de DisponibilidadSnapshot")
class DisponibilidadSnapshotTest {

    private MedicoId medicoId;
    private DisponibilidadSnapshot snapshot;

    @BeforeEach
    void setUp() {
        medicoId = MedicoId.of(1L);
        snapshot = new DisponibilidadSnapshot(medicoId, 30);

        // Agregar horario para LUNES de 8:00 a 17:00
        snapshot.agregarHorarioSemanal(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0)));
    }

    @Test
    @DisplayName("Debería crear snapshot con intervalo correcto")
    void testCrear() {
        assertEquals(medicoId, snapshot.getMedicoId());
        assertEquals(30, snapshot.getIntervaloMinutos());
    }

    @Test
    @DisplayName("Debería detectar disponibilidad en horario válido")
    void testEstaDisponibleTrue() {
        LocalDateTime fechaHora = LocalDateTime.of(2026, 5, 11, 10, 0, 0); // Lunes 10:00
        assertTrue(snapshot.estaDisponible(medicoId, fechaHora));
    }

    @Test
    @DisplayName("No debería estar disponible fuera del horario laboral")
    void testEstaDisponibleFalse() {
        LocalDateTime fechaHora = LocalDateTime.of(2026, 5, 11, 19, 0, 0); // Lunes 19:00 (fuera de horario)
        assertFalse(snapshot.estaDisponible(medicoId, fechaHora));
    }

    @Test
    @DisplayName("No debería estar disponible si el médico es diferente")
    void testEstaDisponibleMedicoDiferente() {
        MedicoId otroMedico = MedicoId.of(2L);
        LocalDateTime fechaHora = LocalDateTime.of(2026, 5, 11, 10, 0, 0);
        assertFalse(snapshot.estaDisponible(otroMedico, fechaHora));
    }

    @Test
    @DisplayName("Debería detectar disponibilidad solo en slots que coinciden con el intervalo")
    void testEsSlotValido() {
        LocalDateTime fechaHora = LocalDateTime.of(2026, 5, 11, 10, 0, 0);
        assertTrue(snapshot.esSlotValido(fechaHora));

        fechaHora = LocalDateTime.of(2026, 5, 11, 10, 15, 0);
        assertFalse(snapshot.esSlotValido(fechaHora));
    }

    @Test
    @DisplayName("Debería agregar bloques específicos y evitar disponibilidad allí")
    void testBloqueosEspecificos() {
        LocalDateTime fechaHora = LocalDateTime.of(2026, 5, 11, 10, 0, 0);
        assertTrue(snapshot.estaDisponible(medicoId, fechaHora));

        snapshot.agregarBloqueo(fechaHora);
        assertFalse(snapshot.estaDisponible(medicoId, fechaHora));
    }
}