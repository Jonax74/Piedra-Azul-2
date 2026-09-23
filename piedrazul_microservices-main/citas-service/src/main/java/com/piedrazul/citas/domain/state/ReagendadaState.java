package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.exception.CitaNoMarcableException;
import com.piedrazul.citas.domain.model.EstadoCita;

import java.time.LocalDateTime;

final class ReagendadaState extends AbstractEstadoCitaState {

    static final ReagendadaState INSTANCE = new ReagendadaState();

    private ReagendadaState() {
        super(EstadoCita.REAGENDADA);
    }

    @Override
    public boolean puedeCancelar() {
        return true;
    }

    @Override
    public boolean puedeMarcarComoAtendida(LocalDateTime fechaCita) {
        return fechaHoraYaPaso(fechaCita);
    }

    @Override
    public boolean puedeMarcarComoNoAsistida(LocalDateTime fechaCita) {
        return fechaHoraYaPaso(fechaCita);
    }

    @Override
    public void cancelar(EstadoCitaContext contexto, String motivo) {
        contexto.establecerMotivoCancelacion(motivo);
        contexto.aplicarEstado(EstadoCita.CANCELADA);
    }

    @Override
    public void marcarComoAtendida(EstadoCitaContext contexto) {
        if (!puedeMarcarComoAtendida(contexto.getFechaHora())) {
            throw new CitaNoMarcableException("No se puede marcar como atendida una cita futura");
        }
        contexto.establecerFechaAsistencia(LocalDateTime.now());
        contexto.aplicarEstado(EstadoCita.ATENDIDA);
    }

    @Override
    public void marcarComoNoAsistida(EstadoCitaContext contexto) {
        if (!puedeMarcarComoNoAsistida(contexto.getFechaHora())) {
            throw new CitaNoMarcableException("No se puede marcar como no asistida una cita futura");
        }
        contexto.establecerFechaAsistencia(LocalDateTime.now());
        contexto.aplicarEstado(EstadoCita.NO_ASISTIDA);
    }
}
