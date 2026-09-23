package com.piedrazul.citas.application.service.agendamiento;

import com.piedrazul.citas.application.dto.request.CrearCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.application.mapper.CitaApplicationMapper;
import com.piedrazul.citas.application.port.outgoing.*;
import com.piedrazul.citas.domain.exception.*;
import com.piedrazul.citas.domain.factory.CitaBuilderFactory;
import com.piedrazul.citas.domain.model.*;
import com.piedrazul.citas.domain.policy.CitaProgramadaUnicaPolicy;
import com.piedrazul.citas.domain.policy.ConsultaGeneralPolicy;
import com.piedrazul.citas.domain.valueobjects.*;
import com.piedrazul.citas.application.service.singleton.ConfiguracionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
public abstract class AbstractAgendamientoService {

    protected final CitaRepositoryPort citaRepository;
    protected final PacienteSnapshotRepositoryPort pacienteRepository;
    protected final MedicoSnapshotRepositoryPort medicoRepository;
    protected final DisponibilidadSnapshotRepositoryPort disponibilidadRepository;
    protected final CitaEventPublisherPort eventPublisher;
    protected final CitaApplicationMapper mapper;
    protected final CitaBuilderFactory builderFactory;
    protected final ConfiguracionManager configuracionManager;

    // Template Method
    public final CitaResponse crearCita(CrearCitaRequest request) {

        PacienteId pacienteId = PacienteId.of(request.getPacienteId());
        MedicoId medicoId = MedicoId.of(request.getMedicoId());
        UsuarioId creadoPor = UsuarioId.of(request.getUsuarioCreadorId());

        PacienteSnapshot paciente = obtenerPaciente(pacienteId);

        MedicoSnapshot medico = obtenerMedico(medicoId);

        DisponibilidadSnapshot disponibilidad =
                obtenerDisponibilidad(medicoId);

        validarHorarioDisponible(
                pacienteId,
                medicoId,
                request.getFechaHora(),
                disponibilidad
        );

        validarTipoAgendamiento(
                request,
                paciente,
                medico
        );

        ConsultaGeneralPolicy.validarAgendamiento(
                request.getEspecialidad(),
                citaRepository.existeConsultaGeneralAtendidaByPacienteId(pacienteId)
        );

        CitaProgramadaUnicaPolicy.validar(
                citaRepository.existeCitaProgramadaByPacienteId(pacienteId)
        );

        if (request.getEspecialidad() == null) {
            throw new IllegalArgumentException("La especialidad de la cita es obligatoria");
        }

        if (!medico.tieneEspecialidad(request.getEspecialidad())) {
            throw new MedicoNoDisponibleException(
                    "El médico no atiende la especialidad solicitada: " + request.getEspecialidad()
            );
        }

        Cita cita = builderFactory.crearBuilder()
                .conPaciente(pacienteId, paciente)
                .conMedico(medicoId, medico)
                .conEspecialidad(request.getEspecialidad())
                .conMotivoAgendamiento(normalizarMotivoAgendamiento(request.getMotivoAgendamiento()))
                .creadaPor(creadoPor)
                .paraFecha(request.getFechaHora())
                .conDisponibilidad(disponibilidad)
                .build();

        try {

            Cita guardada = citaRepository.save(cita);

            eventPublisher.publicarCitaAgendada(guardada, paciente, medico);

            return mapper.toResponse(
                    guardada,
                    paciente,
                    medico
            );

        } catch (DataIntegrityViolationException e) {

            throw new DisponibilidadNoDisponibleException(
                    "El horario seleccionado ya no está disponible"
            );
        }
    }

    // TEMPLATE HOOKS
    protected abstract void validarTipoAgendamiento(
            CrearCitaRequest request,
            PacienteSnapshot paciente,
            MedicoSnapshot medico
    );

    // MÉTODOS COMUNES
    private PacienteSnapshot obtenerPaciente(PacienteId pacienteId) {

        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() ->
                        new PacienteNoExisteException(
                                "Paciente no encontrado"
                        ));
    }

    private MedicoSnapshot obtenerMedico(MedicoId medicoId) {

        return medicoRepository.findById(medicoId)
                .orElseThrow(() ->
                        new MedicoNoDisponibleException(
                                "Médico no encontrado"
                        ));
    }

    private DisponibilidadSnapshot obtenerDisponibilidad(
            MedicoId medicoId
    ) {

        return disponibilidadRepository
                .findByMedicoId(medicoId)
                .orElseThrow(() ->
                        new DisponibilidadNoDisponibleException(
                                "No hay disponibilidad"
                        ));
    }

    private static String normalizarMotivoAgendamiento(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        return motivo.trim();
    }

    private void validarHorarioDisponible(
            PacienteId pacienteId,
            MedicoId medicoId,
            java.time.LocalDateTime fechaHora,
            DisponibilidadSnapshot disponibilidad
    ) {

        if (citaRepository.existsCitaActivaByMedicoIdAndFechaHora(
                medicoId,
                fechaHora
        )) {

            throw new DisponibilidadNoDisponibleException(
                    "El médico ya tiene una cita agendada en el horario seleccionado"
            );
        }

        if (citaRepository.existsCitaActivaByPacienteIdAndFechaHora(
                pacienteId,
                fechaHora
        )) {

            throw new PacienteNoDisponibleException(
                    "El paciente ya tiene una cita agendada en el horario seleccionado"
            );
        }

        if (configuracionManager.obtenerConfiguracion().esFestivo(fechaHora.toLocalDate())) {
            throw new DisponibilidadNoDisponibleException(
                    "La fecha seleccionada es un día festivo y no está disponible para agendamiento"
            );
        }

        if (!disponibilidad.esSlotValido(fechaHora)) {

            throw new DisponibilidadNoDisponibleException(
                    "El horario seleccionado no está disponible"
            );
        }
    }
}