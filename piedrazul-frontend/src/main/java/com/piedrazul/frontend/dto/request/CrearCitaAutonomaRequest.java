package com.piedrazul.frontend.dto.request;

import java.time.LocalDateTime;

public class CrearCitaAutonomaRequest {

    private Long pacienteId;
    private Long medicoId;
    private Long usuarioCreadorId;
    private LocalDateTime fechaHora;
    private String especialidad;
    private String motivoAgendamiento;

    public CrearCitaAutonomaRequest() {
    }

    public CrearCitaAutonomaRequest(Long pacienteId, Long medicoId, Long usuarioCreadorId,
                                    LocalDateTime fechaHora, String especialidad) {
        this(pacienteId, medicoId, usuarioCreadorId, fechaHora, especialidad, null);
    }

    public CrearCitaAutonomaRequest(Long pacienteId, Long medicoId, Long usuarioCreadorId,
                                    LocalDateTime fechaHora, String especialidad,
                                    String motivoAgendamiento) {
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.usuarioCreadorId = usuarioCreadorId;
        this.fechaHora = fechaHora;
        this.especialidad = especialidad;
        this.motivoAgendamiento = motivoAgendamiento;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public Long getUsuarioCreadorId() {
        return usuarioCreadorId;
    }

    public void setUsuarioCreadorId(Long usuarioCreadorId) {
        this.usuarioCreadorId = usuarioCreadorId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getMotivoAgendamiento() {
        return motivoAgendamiento;
    }

    public void setMotivoAgendamiento(String motivoAgendamiento) {
        this.motivoAgendamiento = motivoAgendamiento;
    }
}
