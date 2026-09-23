package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.state.EstadoCitaContext;
import com.piedrazul.citas.domain.state.EstadoCitaState;
import com.piedrazul.citas.domain.state.EstadoCitaStateFactory;
import com.piedrazul.citas.domain.valueobjects.*;
import java.time.LocalDateTime;

public class Cita implements EstadoCitaContext {

    private final CitaId id;
    private final PacienteId pacienteId;
    private final MedicoId medicoId;
    private final EspecialidadMedica especialidad;
    private final UsuarioId creadoPor;
    private LocalDateTime fechaHora;
    private EstadoCita estado;
    private EstadoCitaState comportamiento;
    private String motivoAgendamiento;
    private String motivoCancelacion;
    private LocalDateTime fechaAsistencia;
    private AuditMetadata audit;

    public Cita(CitaId id, PacienteId pacienteId, MedicoId medicoId,
                EspecialidadMedica especialidad, UsuarioId creadoPor,
                LocalDateTime fechaHora, String motivoAgendamiento) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.especialidad = especialidad;
        this.creadoPor = creadoPor;
        this.fechaHora = fechaHora;
        this.motivoAgendamiento = motivoAgendamiento;
        inicializarEstado(EstadoCita.PROGRAMADA);
        this.audit = AuditMetadata.crear();
    }

    private Cita(CitaId id, PacienteId pacienteId, MedicoId medicoId,
                 EspecialidadMedica especialidad, UsuarioId creadoPor, LocalDateTime fechaHora,
                 EstadoCita estado, String motivoAgendamiento, String motivoCancelacion,
                 LocalDateTime fechaAsistencia, AuditMetadata audit) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.especialidad = especialidad;
        this.creadoPor = creadoPor;
        this.fechaHora = fechaHora;
        this.motivoAgendamiento = motivoAgendamiento;
        this.motivoCancelacion = motivoCancelacion;
        this.fechaAsistencia = fechaAsistencia;
        this.audit = audit;
        inicializarEstado(estado);
    }

    public static Cita reconstruir(CitaId id, PacienteId pacienteId, MedicoId medicoId,
                                   EspecialidadMedica especialidad, UsuarioId creadoPor,
                                   LocalDateTime fechaHora, EstadoCita estado,
                                   String motivoAgendamiento, String motivoCancelacion,
                                   LocalDateTime fechaAsistencia,
                                   LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy) {

        AuditMetadata audit = AuditMetadata.reconstruir(createdAt, updatedAt, createdBy);

        return new Cita(id, pacienteId, medicoId, especialidad, creadoPor, fechaHora,
                estado, motivoAgendamiento, motivoCancelacion, fechaAsistencia, audit);
    }

    private void inicializarEstado(EstadoCita nuevoEstado) {
        this.estado = nuevoEstado;
        this.comportamiento = EstadoCitaStateFactory.of(nuevoEstado);
    }

    @Override
    public void aplicarEstado(EstadoCita nuevoEstado) {
        inicializarEstado(nuevoEstado);
        this.audit.actualizar();
    }

    @Override
    public void establecerFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
        this.audit.actualizar();
    }

    @Override
    public void establecerMotivoCancelacion(String motivo) {
        this.motivoCancelacion = motivo;
        this.audit.actualizar();
    }

    @Override
    public void establecerFechaAsistencia(LocalDateTime fechaAsistencia) {
        this.fechaAsistencia = fechaAsistencia;
        this.audit.actualizar();
    }

    public void cancelar(String motivo) {
        comportamiento.cancelar(this, motivo);
    }

    public void reagendar(LocalDateTime nuevaFechaHora, DisponibilidadSnapshot disponibilidad) {
        comportamiento.reagendarEnMismaCita(this, nuevaFechaHora, disponibilidad);
    }

    public boolean reagendamientoCreaNuevaCita() {
        return comportamiento.puedeReagendarCreandoNuevaCita();
    }

    public void marcarComoAtendida() {
        comportamiento.marcarComoAtendida(this);
    }

    public void marcarComoNoAsistida() {
        comportamiento.marcarComoNoAsistida(this);
    }

    public CitaId getId() { return id; }
    public PacienteId getPacienteId() { return pacienteId; }
    @Override
    public MedicoId getMedicoId() { return medicoId; }
    public EspecialidadMedica getEspecialidad() { return especialidad; }
    public UsuarioId getCreadoPor() { return creadoPor; }
    @Override
    public LocalDateTime getFechaHora() { return fechaHora; }
    @Override
    public EstadoCita getEstado() { return estado; }
    public String getMotivoAgendamiento() { return motivoAgendamiento; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public LocalDateTime getFechaAsistencia() { return fechaAsistencia; }
    public AuditMetadata getAudit() { return audit; }
}
