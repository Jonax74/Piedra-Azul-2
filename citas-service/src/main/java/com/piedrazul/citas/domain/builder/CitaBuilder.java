package com.piedrazul.citas.domain.builder;

import com.piedrazul.citas.domain.exception.DisponibilidadNoDisponibleException;
import com.piedrazul.citas.domain.exception.MedicoNoDisponibleException;
import com.piedrazul.citas.domain.exception.PacienteNoExisteException;
import com.piedrazul.citas.domain.model.*;
import com.piedrazul.citas.domain.valueobjects.*;

import java.time.LocalDateTime;

public abstract class CitaBuilder {

    protected PacienteId pacienteId;
    protected MedicoId medicoId;
    protected UsuarioId creadoPor;
    protected LocalDateTime fechaHora;

    protected PacienteSnapshot paciente;
    protected MedicoSnapshot medico;
    protected DisponibilidadSnapshot disponibilidad;
    protected EspecialidadMedica especialidad;
    protected String motivoAgendamiento;

    public CitaBuilder conPaciente(PacienteId pacienteId,
                                   PacienteSnapshot paciente) {

        this.pacienteId = pacienteId;
        this.paciente = paciente;
        return this;
    }

    public CitaBuilder conMedico(MedicoId medicoId,
                                 MedicoSnapshot medico) {

        this.medicoId = medicoId;
        this.medico = medico;
        return this;
    }

    public CitaBuilder creadaPor(UsuarioId usuarioId) {
        this.creadoPor = usuarioId;
        return this;
    }

    public CitaBuilder paraFecha(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
        return this;
    }

    public CitaBuilder conDisponibilidad(
            DisponibilidadSnapshot disponibilidad
    ) {
        this.disponibilidad = disponibilidad;
        return this;
    }

    public CitaBuilder conEspecialidad(EspecialidadMedica especialidad) {
        this.especialidad = especialidad;
        return this;
    }

    public CitaBuilder conMotivoAgendamiento(String motivoAgendamiento) {
        this.motivoAgendamiento = motivoAgendamiento;
        return this;
    }

    public abstract Cita build();

    protected void validarPacienteActivo() {

        if (!paciente.existe()) {

            throw new PacienteNoExisteException(
                    "Paciente no encontrado"
            );
        }
    }

    protected void validarMedicoActivo() {

        if (!medico.estaActivo()) {

            throw new MedicoNoDisponibleException(
                    "Médico inactivo"
            );
        }
    }

    protected void validarEspecialidad() {
        if (especialidad == null) {
            throw new IllegalArgumentException("La especialidad de la cita es obligatoria");
        }
    }

    protected void validarMedicoAtiendeEspecialidad() {
        if (!medico.tieneEspecialidad(especialidad)) {
            throw new MedicoNoDisponibleException(
                    "El médico no atiende la especialidad solicitada: " + especialidad
            );
        }
    }

    protected void validarDisponibilidad() {

        if (!disponibilidad.estaDisponible(
                medicoId,
                fechaHora
        )) {

            throw new DisponibilidadNoDisponibleException(
                    "Horario no disponible"
            );
        }
    }

    protected Cita construir() {

        return new Cita(
                CitaId.generate(),
                pacienteId,
                medicoId,
                especialidad,
                creadoPor,
                fechaHora,
                motivoAgendamiento
        );
    }
}