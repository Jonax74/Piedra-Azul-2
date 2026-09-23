package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.exception.CitaNoCancelableException;
import com.piedrazul.citas.domain.exception.CitaNoMarcableException;
import com.piedrazul.citas.domain.exception.CitaNoReagendableException;
import com.piedrazul.citas.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Cita")
class CitaTest {

    private CitaId citaId;
    private PacienteId pacienteId;
    private MedicoId medicoId;
    private UsuarioId creadoPor;
    private LocalDateTime fechaHora;
    private Cita cita;

    @BeforeEach
    void setUp() {
        citaId = CitaId.generate();
        pacienteId = PacienteId.of(1L);
        medicoId = MedicoId.of(1L);
        creadoPor = UsuarioId.of(1L);
        fechaHora = LocalDateTime.now().plusDays(2);

        cita = new Cita(citaId, pacienteId, medicoId, EspecialidadMedica.GENERAL, creadoPor, fechaHora, null);
    }

    @Test
    @DisplayName("Debería crear una cita en estado PROGRAMADA")
    void testCrearCita() {
        assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
    }

    @Test
    @DisplayName("PROGRAMADA puede cancelarse")
    void testCancelarDesdeProgramada() {
        cita.cancelar("Paciente no pudo asistir");
        assertEquals(EstadoCita.CANCELADA, cita.getEstado());
        assertEquals("Paciente no pudo asistir", cita.getMotivoCancelacion());
    }

    @Test
    @DisplayName("CANCELADA es estado final")
    void testCancelarCitaYaCancelada() {
        cita.cancelar("Motivo");
        assertThrows(CitaNoCancelableException.class, () -> cita.cancelar("Otro motivo"));
    }

    @Test
    @DisplayName("PROGRAMADA puede reagendarse en la misma cita")
    void testReagendarDesdeProgramada() {
        LocalDateTime nuevaFecha = fechaLaborable(LocalDateTime.now().plusDays(5));
        DisponibilidadSnapshot disponibilidad = crearDisponibilidadSnapshot(medicoId, nuevaFecha);

        cita.reagendar(nuevaFecha, disponibilidad);

        assertEquals(EstadoCita.REAGENDADA, cita.getEstado());
        assertEquals(nuevaFecha, cita.getFechaHora());
    }

    @Test
    @DisplayName("REAGENDADA no puede reagendarse nuevamente")
    void testReagendarDesdeReagendada() {
        cita = reconstruirCitaConEstado(EstadoCita.REAGENDADA);
        DisponibilidadSnapshot disponibilidad = crearDisponibilidadSnapshot(medicoId, fechaHora);

        assertThrows(CitaNoReagendableException.class, () ->
                cita.reagendar(LocalDateTime.now().plusDays(7), disponibilidad)
        );
    }

    @Test
    @DisplayName("PROGRAMADA pasada puede marcarse como atendida")
    void testMarcarComoAtendidaDesdeProgramada() {
        cita = reconstruirCitaConEstado(EstadoCita.PROGRAMADA, LocalDateTime.now().minusHours(2));
        cita.marcarComoAtendida();
        assertEquals(EstadoCita.ATENDIDA, cita.getEstado());
        assertNotNull(cita.getFechaAsistencia());
    }

    @Test
    @DisplayName("PROGRAMADA futura no puede marcarse como atendida")
    void testMarcarComoAtendidaFutura() {
        assertThrows(CitaNoMarcableException.class, cita::marcarComoAtendida);
    }

    @Test
    @DisplayName("ATENDIDA no puede marcarse como atendida nuevamente")
    void testMarcarComoAtendidaDesdeAtendida() {
        cita = reconstruirCitaConEstado(EstadoCita.ATENDIDA);
        assertThrows(CitaNoMarcableException.class, cita::marcarComoAtendida);
    }

    @Test
    @DisplayName("PROGRAMADA futura no puede marcarse como no asistida")
    void testMarcarComoNoAsistidaFutura() {
        assertThrows(CitaNoMarcableException.class, cita::marcarComoNoAsistida);
    }

    @Test
    @DisplayName("REAGENDADA pasada puede marcarse como no asistida")
    void testMarcarComoNoAsistidaDesdeReagendada() {
        cita = reconstruirCitaConEstado(EstadoCita.REAGENDADA, LocalDateTime.now().minusHours(2));

        cita.marcarComoNoAsistida();

        assertEquals(EstadoCita.NO_ASISTIDA, cita.getEstado());
    }

    @Test
    @DisplayName("REAGENDADA futura no puede marcarse como no asistida")
    void testMarcarComoNoAsistidaReagendadaFutura() {
        cita = reconstruirCitaConEstado(EstadoCita.REAGENDADA, LocalDateTime.now().plusDays(2));
        assertThrows(CitaNoMarcableException.class, cita::marcarComoNoAsistida);
    }

    @Test
    @DisplayName("ATENDIDA indica que el reagendamiento crea una nueva cita")
    void testReagendamientoDesdeAtendidaCreaNuevaCita() {
        cita = reconstruirCitaConEstado(EstadoCita.ATENDIDA);
        assertTrue(cita.reagendamientoCreaNuevaCita());
    }

    private Cita reconstruirCitaConEstado(EstadoCita estado) {
        return reconstruirCitaConEstado(estado, fechaHora);
    }

    private Cita reconstruirCitaConEstado(EstadoCita estado, LocalDateTime fecha) {
        return Cita.reconstruir(
                citaId, pacienteId, medicoId, EspecialidadMedica.GENERAL, creadoPor, fecha,
                estado, null, null, null,
                LocalDateTime.now(), LocalDateTime.now(), "system"
        );
    }

    private LocalDateTime fechaLaborable(LocalDateTime base) {
        LocalDateTime fecha = base.withHour(10).withMinute(0).withSecond(0).withNano(0);
        while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private DisponibilidadSnapshot crearDisponibilidadSnapshot(MedicoId medicoId, LocalDateTime fechaHora) {
        DisponibilidadSnapshot snapshot = new DisponibilidadSnapshot(medicoId, 30);
        DayOfWeek dia = fechaHora.getDayOfWeek();
        LocalTime hora = fechaHora.toLocalTime();
        LocalTime start = hora.minusMinutes(hora.getMinute());
        LocalTime end = start.plusHours(2);
        snapshot.agregarHorarioSemanal(dia, new TimeRange(start, end));
        return snapshot;
    }
}
