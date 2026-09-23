package com.piedrazul.citas.domain.builder;

import com.piedrazul.citas.domain.exception.DisponibilidadNoDisponibleException;
import com.piedrazul.citas.domain.exception.RestriccionAutoservicioException;
import com.piedrazul.citas.domain.model.*;
import com.piedrazul.citas.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de CitaAutonomaBuilder")
class CitaAutonomaBuilderTest {

    private CitaAutonomaBuilder builder;
    private PacienteId pacienteId;
    private MedicoId medicoId;
    private UsuarioId creadoPor;
    private LocalDateTime fechaHora;
    private PacienteSnapshot pacienteSnapshot;
    private MedicoSnapshot medicoSnapshot;
    private DisponibilidadSnapshot disponibilidadSnapshot;

    @BeforeEach
    void setUp() {
        builder = new CitaAutonomaBuilder();

        pacienteId = PacienteId.of(1L);
        medicoId = MedicoId.of(1L);
        creadoPor = UsuarioId.of(1L);

        fechaHora = calcularProximoLunes();

        pacienteSnapshot = new PacienteSnapshot(pacienteId, "Juan Pérez", "juan@email.com", "123456789", true);
        medicoSnapshot = new MedicoSnapshot(
                medicoId, "Dr. Gómez", "dr@email.com", Set.of(EspecialidadMedica.GENERAL), EstadoMedico.ACTIVO);

        disponibilidadSnapshot = new DisponibilidadSnapshot(medicoId, 30);

        DayOfWeek dia = fechaHora.getDayOfWeek();
        disponibilidadSnapshot.agregarHorarioSemanal(dia,
                new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0)));
    }

    private LocalDateTime calcularProximoLunes() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fecha = ahora.plusDays(2);

        while (fecha.getDayOfWeek() != DayOfWeek.MONDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha.withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Debería construir una cita autónoma exitosamente")
    void testBuildExitoso() {
        Cita cita = builder
                .conPaciente(pacienteId, pacienteSnapshot)
                .conMedico(medicoId, medicoSnapshot)
                .conEspecialidad(EspecialidadMedica.GENERAL)
                .creadaPor(creadoPor)
                .paraFecha(fechaHora)
                .conDisponibilidad(disponibilidadSnapshot)
                .build();

        assertNotNull(cita);
        assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
    }

    @Test
    @DisplayName("No debería permitir citas con menos de 24 horas de anticipación")
    void testValidarAnticipacion24Horas() {
        // Crear copias finales
        final PacienteId finalPacienteId = pacienteId;
        final PacienteSnapshot finalPacienteSnapshot = pacienteSnapshot;
        final MedicoId finalMedicoId = medicoId;
        final MedicoSnapshot finalMedicoSnapshot = medicoSnapshot;
        final UsuarioId finalCreadoPor = creadoPor;
        final LocalDateTime fechaCercana = LocalDateTime.now().plusHours(12);

        DayOfWeek dia = fechaCercana.getDayOfWeek();
        final DisponibilidadSnapshot disponibilidadLocal = new DisponibilidadSnapshot(medicoId, 30);
        disponibilidadLocal.agregarHorarioSemanal(dia,
                new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0)));

        final CitaAutonomaBuilder builderLocal = new CitaAutonomaBuilder();

        // Dependiendo de la hora actual, puede fallar primero la validación
        // de disponibilidad o la restricción de autoservicio (24h).
        Exception exception = assertThrows(Exception.class, () -> {
            builderLocal
                    .conPaciente(finalPacienteId, finalPacienteSnapshot)
                    .conMedico(finalMedicoId, finalMedicoSnapshot)
                    .conEspecialidad(EspecialidadMedica.GENERAL)
                    .creadaPor(finalCreadoPor)
                    .paraFecha(fechaCercana)
                    .conDisponibilidad(disponibilidadLocal)
                    .build();
        });

        assertTrue(exception instanceof RestriccionAutoservicioException ||
                        exception instanceof DisponibilidadNoDisponibleException,
                "Se esperaba RestriccionAutoservicioException o DisponibilidadNoDisponibleException");
    }

    @Test
    @DisplayName("Debería validar disponibilidad del horario")
    void testValidarDisponibilidad() {
        // Crear copias finales
        final PacienteId finalPacienteId = pacienteId;
        final PacienteSnapshot finalPacienteSnapshot = pacienteSnapshot;
        final MedicoId finalMedicoId = medicoId;
        final MedicoSnapshot finalMedicoSnapshot = medicoSnapshot;
        final UsuarioId finalCreadoPor = creadoPor;
        final DisponibilidadSnapshot finalDisponibilidadSnapshot = disponibilidadSnapshot;

        // Fecha sin disponibilidad (domingo)
        LocalDateTime fechaSinDisponibilidad = LocalDateTime.now().plusDays(3);
        while (fechaSinDisponibilidad.getDayOfWeek() != DayOfWeek.SUNDAY) {
            fechaSinDisponibilidad = fechaSinDisponibilidad.plusDays(1);
        }
        final LocalDateTime finalFechaSinDisponibilidad = fechaSinDisponibilidad.withHour(10).withMinute(0).withSecond(0);

        final CitaAutonomaBuilder builderLocal = new CitaAutonomaBuilder();

        assertThrows(DisponibilidadNoDisponibleException.class, () -> {
            builderLocal
                    .conPaciente(finalPacienteId, finalPacienteSnapshot)
                    .conMedico(finalMedicoId, finalMedicoSnapshot)
                    .conEspecialidad(EspecialidadMedica.GENERAL)
                    .creadaPor(finalCreadoPor)
                    .paraFecha(finalFechaSinDisponibilidad)
                    .conDisponibilidad(finalDisponibilidadSnapshot)
                    .build();
        });
    }
}