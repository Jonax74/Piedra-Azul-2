package com.piedrazul.citas.domain.model;

public enum EstadoMedico {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    VACACIONES("Vacaciones"),
    CAPACITACION("Capacitación");

    private final String descripcion;

    EstadoMedico(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}