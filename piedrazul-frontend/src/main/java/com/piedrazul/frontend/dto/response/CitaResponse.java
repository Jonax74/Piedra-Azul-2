package com.piedrazul.frontend.dto.response;

public class CitaResponse {

    private String id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private String fechaHora;
    private String estado;
    private String motivoAgendamiento;
    private String motivoCancelacion;
    private String fechaAsistencia;
    private String createdAt;
    private String updatedAt;

    // GETTERS Y SETTERS

    public String getId() {
        return id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public String getMedicoNombre() {
        return medicoNombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivoAgendamiento() {
        return motivoAgendamiento;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public String getFechaAsistencia() {
        return fechaAsistencia;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public void setMedicoNombre(String medicoNombre) {
        this.medicoNombre = medicoNombre;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setMotivoAgendamiento(String motivoAgendamiento) {
        this.motivoAgendamiento = motivoAgendamiento;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public void setFechaAsistencia(String fechaAsistencia) {
        this.fechaAsistencia = fechaAsistencia;
    }

    public String getCreatedAt() { return createdAt; }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}