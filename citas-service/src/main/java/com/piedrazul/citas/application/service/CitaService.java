package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.dto.request.*;
import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.application.mapper.CitaApplicationMapper;
import com.piedrazul.citas.application.port.incoming.*;
import com.piedrazul.citas.application.port.outgoing.*;
import com.piedrazul.citas.domain.exception.*;
import com.piedrazul.citas.domain.model.*;
import com.piedrazul.citas.domain.policy.CitaProgramadaUnicaPolicy;
import com.piedrazul.citas.domain.valueobjects.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.piedrazul.citas.application.service.singleton.ConfiguracionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CitaService implements CancelarCitaUseCase,
        ReagendarCitaUseCase, MarcarAsistenciaUseCase, ListarCitasUseCase {

    private final CitaRepositoryPort citaRepository;
    private final PacienteSnapshotRepositoryPort pacienteSnapshotRepository;
    private final MedicoSnapshotRepositoryPort medicoSnapshotRepository;
    private final DisponibilidadSnapshotRepositoryPort disponibilidadSnapshotRepository;
    private final CitaEventPublisherPort eventPublisher;
    private final CitaApplicationMapper mapper;
    private final ConfiguracionManager configuracionManager;

    @Override
    public CitaResponse cancelarCita(CancelarCitaRequest request) {
        log.info("Cancelando cita: {}", request.getCitaId());

        CitaId citaId = CitaId.fromString(request.getCitaId());

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException(
                        "Cita no encontrada con ID: " + request.getCitaId()));

        cita.cancelar(request.getMotivo());

        Cita citaActualizada = citaRepository.save(cita);
        log.info("Cita cancelada exitosamente: {}", citaActualizada.getId());

        // Snapshots para enriquecer el evento y la respuesta
        PacienteSnapshot paciente = pacienteSnapshotRepository.findById(cita.getPacienteId()).orElse(null);
        MedicoSnapshot medico = medicoSnapshotRepository.findById(cita.getMedicoId()).orElse(null);

        eventPublisher.publicarCitaCancelada(citaActualizada, paciente, medico);

        return mapper.toResponse(citaActualizada, paciente, medico);
    }

    @Override
    public CitaResponse reagendarCita(ReagendarCitaRequest request) {
        log.info("Reagendando cita: {} para nueva fecha: {}",
                request.getCitaId(), request.getNuevaFechaHora());

        CitaId citaId = CitaId.fromString(request.getCitaId());

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException(
                        "Cita no encontrada con ID: " + request.getCitaId()));

        DisponibilidadSnapshot disponibilidad = disponibilidadSnapshotRepository
                .findByMedicoId(cita.getMedicoId())
                .orElseThrow(() -> new DisponibilidadNoDisponibleException(
                        "No hay disponibilidad configurada para el médico"));

        LocalDateTime nuevaFechaHora = request.getNuevaFechaHora();
        validarNuevoHorario(cita, nuevaFechaHora, cita.getId());

        if (cita.reagendamientoCreaNuevaCita()) {
            return crearCitaSeguimientoDesdeAtendida(cita, nuevaFechaHora, disponibilidad);
        }

        LocalDateTime fechaHoraOriginal = cita.getFechaHora();
        cita.reagendar(nuevaFechaHora, disponibilidad);

        Cita citaActualizada = citaRepository.save(cita);
        log.info("Cita reagendada exitosamente: {} nueva fecha: {}",
                citaActualizada.getId(), citaActualizada.getFechaHora());

        PacienteSnapshot paciente = pacienteSnapshotRepository.findById(cita.getPacienteId()).orElse(null);
        MedicoSnapshot medico = medicoSnapshotRepository.findById(cita.getMedicoId()).orElse(null);

        eventPublisher.publicarCitaReagendada(citaActualizada, fechaHoraOriginal, paciente, medico);

        return mapper.toResponse(citaActualizada, paciente, medico);
    }

    @Override
    public CitaResponse marcarAsistencia(MarcarAsistenciaRequest request) {
        log.info("Marcando asistencia para cita: {}, asistió: {}",
                request.getCitaId(), request.isAsistio());

        CitaId citaId = CitaId.fromString(request.getCitaId());

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException(
                        "Cita no encontrada con ID: " + request.getCitaId()));

        if (request.isAsistio()) {
            cita.marcarComoAtendida();
            log.info("Cita marcada como ATENDIDA: {}", cita.getId());
        } else {
            cita.marcarComoNoAsistida();
            log.info("Cita marcada como NO_ASISTIDA: {}", cita.getId());
        }

        Cita citaActualizada = citaRepository.save(cita);

        PacienteSnapshot paciente = pacienteSnapshotRepository.findById(cita.getPacienteId()).orElse(null);
        MedicoSnapshot medico = medicoSnapshotRepository.findById(cita.getMedicoId()).orElse(null);

        return mapper.toResponse(citaActualizada, paciente, medico);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listar(Long medicoId, Long pacienteId, LocalDate fechaInicio, LocalDate fechaFin) {

        List<Cita> citas;

        if (pacienteId != null) {
            citas = citaRepository.findByPacienteId(PacienteId.of(pacienteId));
            if (medicoId != null) {
                MedicoId medicoIdVo = MedicoId.of(medicoId);
                citas = citas.stream()
                        .filter(c -> c.getMedicoId().equals(medicoIdVo))
                        .toList();
            }
        } else if (medicoId != null) {
            citas = citaRepository.findByMedicoId(MedicoId.of(medicoId));
        } else {
            citas = citaRepository.findAll();
        }

        citas = filtrarPorRangoFechas(citas, fechaInicio, fechaFin);

        return citas.stream()
                .map(cita -> {
                    PacienteSnapshot paciente = pacienteSnapshotRepository
                            .findById(cita.getPacienteId()).orElse(null);

                    MedicoSnapshot medico = medicoSnapshotRepository
                            .findById(cita.getMedicoId()).orElse(null);

                    return mapper.toResponse(cita, paciente, medico);
                })
                .toList();
    }

    private CitaResponse crearCitaSeguimientoDesdeAtendida(
            Cita citaOriginal,
            LocalDateTime nuevaFechaHora,
            DisponibilidadSnapshot disponibilidad
    ) {
        validarHorarioLibre(citaOriginal.getMedicoId(), citaOriginal.getPacienteId(), nuevaFechaHora);

        CitaProgramadaUnicaPolicy.validar(
                citaRepository.existeCitaProgramadaByPacienteId(citaOriginal.getPacienteId())
        );

        if (!disponibilidad.estaDisponible(citaOriginal.getMedicoId(), nuevaFechaHora)) {
            throw new DisponibilidadNoDisponibleException("Nuevo horario no disponible");
        }

        Cita nuevaCita = new Cita(
                CitaId.generate(),
                citaOriginal.getPacienteId(),
                citaOriginal.getMedicoId(),
                citaOriginal.getEspecialidad(),
                citaOriginal.getCreadoPor(),
                nuevaFechaHora,
                citaOriginal.getMotivoAgendamiento()
        );

        Cita guardada = citaRepository.save(nuevaCita);
        log.info("Nueva cita creada desde cita atendida {} -> {}",
                citaOriginal.getId(), guardada.getId());

        PacienteSnapshot paciente = pacienteSnapshotRepository.findById(citaOriginal.getPacienteId()).orElse(null);
        MedicoSnapshot medico = medicoSnapshotRepository.findById(citaOriginal.getMedicoId()).orElse(null);

        eventPublisher.publicarCitaAgendada(guardada, paciente, medico);

        return mapper.toResponse(guardada, paciente, medico);
    }

    private void validarHorarioLibre(MedicoId medicoId, PacienteId pacienteId, LocalDateTime nuevaFechaHora) {
        if (citaRepository.existsCitaActivaByMedicoIdAndFechaHora(medicoId, nuevaFechaHora)) {
            throw new DisponibilidadNoDisponibleException(
                    "El médico ya tiene una cita agendada en el horario seleccionado");
        }

        if (citaRepository.existsCitaActivaByPacienteIdAndFechaHora(pacienteId, nuevaFechaHora)) {
            throw new PacienteNoDisponibleException(
                    "El paciente ya tiene una cita agendada en el horario seleccionado");
        }

        if (configuracionManager.obtenerConfiguracion().esFestivo(nuevaFechaHora.toLocalDate())) {
            throw new DisponibilidadNoDisponibleException(
                    "La fecha seleccionada es un día festivo y no está disponible para agendamiento");
        }
    }

    private void validarNuevoHorario(Cita cita, LocalDateTime nuevaFechaHora, CitaId citaIdExcluir) {
        if (citaRepository.existsCitaActivaByMedicoIdAndFechaHoraExcluding(
                cita.getMedicoId(), nuevaFechaHora, citaIdExcluir)) {
            throw new DisponibilidadNoDisponibleException(
                    "El médico ya tiene una cita agendada en el horario seleccionado");
        }

        if (citaRepository.existsCitaActivaByPacienteIdAndFechaHoraExcluding(
                cita.getPacienteId(), nuevaFechaHora, citaIdExcluir)) {
            throw new PacienteNoDisponibleException(
                    "El paciente ya tiene una cita agendada en el horario seleccionado");
        }

        if (configuracionManager.obtenerConfiguracion().esFestivo(nuevaFechaHora.toLocalDate())) {
            throw new DisponibilidadNoDisponibleException(
                    "La fecha seleccionada es un día festivo y no está disponible para agendamiento");
        }
    }

    private List<Cita> filtrarPorRangoFechas(List<Cita> citas, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null && fechaFin == null) {
            return citas;
        }

        LocalDate inicio = fechaInicio != null ? fechaInicio : fechaFin;
        LocalDate fin = fechaFin != null ? fechaFin : fechaInicio;
        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime hasta = fin.atTime(23, 59, 59);

        return citas.stream()
                .filter(c -> c.getFechaHora() != null)
                .filter(c -> !c.getFechaHora().isBefore(desde) && !c.getFechaHora().isAfter(hasta))
                .toList();
    }
}