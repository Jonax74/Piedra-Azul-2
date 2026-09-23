package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.state.EstadoCitaState;
import com.piedrazul.citas.domain.state.EstadoCitaStateFactory;

import java.time.LocalDateTime;

public enum EstadoCita {
    PROGRAMADA("Programada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada"),
    NO_ASISTIDA("No Asistida"),
    REAGENDADA("Reagendada");

    private final String descripcion;

    EstadoCita(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoCitaState comportamiento() {
        return EstadoCitaStateFactory.of(this);
    }

    public boolean esFinal() {
        return comportamiento().esFinal();
    }

    public boolean puedeCancelarse() {
        return comportamiento().puedeCancelar();
    }

    public boolean puedeReagendarseEnMismaCita() {
        return comportamiento().puedeReagendarEnMismaCita();
    }

    public boolean puedeReagendarseCreandoNuevaCita() {
        return comportamiento().puedeReagendarCreandoNuevaCita();
    }

    public boolean puedeMarcarseComoAtendida(LocalDateTime fechaCita) {
        return comportamiento().puedeMarcarComoAtendida(fechaCita);
    }

    public boolean puedeMarcarseComoNoAsistida(LocalDateTime fechaCita) {
        return comportamiento().puedeMarcarComoNoAsistida(fechaCita);
    }

    /**
     * Compatibilidad con registros históricos que aún usan CONFIRMADA en base de datos.
     */
    public static EstadoCita fromPersisted(String valor) {
        if (valor == null || valor.isBlank()) {
            return PROGRAMADA;
        }
        if ("CONFIRMADA".equalsIgnoreCase(valor)) {
            return PROGRAMADA;
        }
        return EstadoCita.valueOf(valor);
    }
}
