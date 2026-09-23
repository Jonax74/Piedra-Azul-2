package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.exception.CitaNoCancelableException;
import com.piedrazul.citas.domain.exception.CitaNoMarcableException;
import com.piedrazul.citas.domain.exception.CitaNoReagendableException;
import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.EstadoCita;

import java.time.LocalDateTime;

abstract class AbstractEstadoCitaState implements EstadoCitaState {

    private final EstadoCita tipo;

    protected AbstractEstadoCitaState(EstadoCita tipo) {
        this.tipo = tipo;
    }

    @Override
    public EstadoCita getTipo() {
        return tipo;
    }

    @Override
    public boolean esFinal() {
        return false;
    }

    @Override
    public boolean puedeCancelar() {
        return false;
    }

    @Override
    public boolean puedeReagendarEnMismaCita() {
        return false;
    }

    @Override
    public boolean puedeReagendarCreandoNuevaCita() {
        return false;
    }

    @Override
    public boolean puedeMarcarComoAtendida(LocalDateTime fechaCita) {
        return false;
    }

    @Override
    public boolean puedeMarcarComoNoAsistida(LocalDateTime fechaCita) {
        return false;
    }

    @Override
    public void cancelar(EstadoCitaContext contexto, String motivo) {
        throw new CitaNoCancelableException(
                "No se puede cancelar una cita en estado: " + tipo.getDescripcion()
        );
    }

    @Override
    public void reagendarEnMismaCita(
            EstadoCitaContext contexto,
            LocalDateTime nuevaFechaHora,
            DisponibilidadSnapshot disponibilidad
    ) {
        throw new CitaNoReagendableException(
                "No se puede reagendar una cita en estado: " + tipo.getDescripcion()
        );
    }

    @Override
    public void marcarComoAtendida(EstadoCitaContext contexto) {
        throw new CitaNoMarcableException(
                "No se puede marcar como atendida una cita en estado: " + tipo.getDescripcion()
        );
    }

    protected static boolean fechaHoraYaPaso(LocalDateTime fechaCita) {
        return fechaCita != null && !fechaCita.isAfter(LocalDateTime.now());
    }

    @Override
    public void marcarComoNoAsistida(EstadoCitaContext contexto) {
        throw new CitaNoMarcableException(
                "No se puede marcar como no asistida una cita en estado: " + tipo.getDescripcion()
        );
    }
}
