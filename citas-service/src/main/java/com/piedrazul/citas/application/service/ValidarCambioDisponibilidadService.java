package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.port.outgoing.CitaRepositoryPort;
import com.piedrazul.citas.application.util.DiaSemanaMapper;
import com.piedrazul.citas.domain.exception.DisponibilidadConCitasActivasException;
import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.model.EstadoCita;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidarCambioDisponibilidadService {

    private static final String MENSAJE_BLOQUEO =
            "Existen citas programadas dentro de esta disponibilidad. "
                    + "Debe atenderlas o reagendarlas antes de poder modificar o eliminar este horario.";

    private final CitaRepositoryPort citaRepository;

    public void validarEliminacion(Long medicoId, String diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        if (tieneCitasActivasEnRango(medicoId, diaSemana, horaInicio, horaFin)) {
            throw new DisponibilidadConCitasActivasException(MENSAJE_BLOQUEO);
        }
    }

    public void validarModificacion(
            Long medicoIdActual,
            String diaSemanaActual,
            LocalTime horaInicioActual,
            LocalTime horaFinActual,
            Long medicoIdNuevo,
            String diaSemanaNuevo,
            LocalTime horaInicioNuevo,
            LocalTime horaFinNuevo
    ) {
        List<Cita> citasAfectadas = obtenerCitasActivasEnRango(
                medicoIdActual, diaSemanaActual, horaInicioActual, horaFinActual
        );

        if (citasAfectadas.isEmpty()) {
            return;
        }

        DayOfWeek diaNuevo = DiaSemanaMapper.toDayOfWeek(diaSemanaNuevo);
        MedicoId medicoNuevo = MedicoId.of(medicoIdNuevo);

        boolean algunaQuedaFuera = citasAfectadas.stream()
                .anyMatch(cita -> !citaEncajaEnNuevoRango(
                        cita, medicoNuevo, diaNuevo, horaInicioNuevo, horaFinNuevo
                ));

        if (algunaQuedaFuera) {
            throw new DisponibilidadConCitasActivasException(MENSAJE_BLOQUEO);
        }
    }

    private boolean tieneCitasActivasEnRango(
            Long medicoId,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        return !obtenerCitasActivasEnRango(medicoId, diaSemana, horaInicio, horaFin).isEmpty();
    }

    private List<Cita> obtenerCitasActivasEnRango(
            Long medicoId,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        DayOfWeek dia = DiaSemanaMapper.toDayOfWeek(diaSemana);
        MedicoId medico = MedicoId.of(medicoId);
        LocalDateTime ahora = LocalDateTime.now();

        return citaRepository.findByMedicoId(medico).stream()
                .filter(cita -> cita.getFechaHora().isAfter(ahora))
                .filter(cita -> cita.getEstado() != EstadoCita.CANCELADA)
                .filter(cita -> cita.getFechaHora().getDayOfWeek() == dia)
                .filter(cita -> horaDentroDeRango(cita.getFechaHora().toLocalTime(), horaInicio, horaFin))
                .toList();
    }

    private boolean citaEncajaEnNuevoRango(
            Cita cita,
            MedicoId medicoIdNuevo,
            DayOfWeek diaNuevo,
            LocalTime horaInicioNuevo,
            LocalTime horaFinNuevo
    ) {
        if (!cita.getMedicoId().equals(medicoIdNuevo)) {
            return false;
        }
        if (cita.getFechaHora().getDayOfWeek() != diaNuevo) {
            return false;
        }
        return horaDentroDeRango(cita.getFechaHora().toLocalTime(), horaInicioNuevo, horaFinNuevo);
    }

    private boolean horaDentroDeRango(LocalTime hora, LocalTime inicio, LocalTime fin) {
        return !hora.isBefore(inicio) && hora.isBefore(fin);
    }
}
