package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;
import com.piedrazul.citas.domain.valueobjects.MedicoId;

import java.time.LocalDateTime;

/**
 * Contexto del patrón State: expone mutaciones controladas del agregado {@code Cita}.
 */
public interface EstadoCitaContext {

    EstadoCita getEstado();

    void aplicarEstado(EstadoCita nuevoEstado);

    void establecerFechaHora(LocalDateTime fechaHora);

    void establecerMotivoCancelacion(String motivo);

    void establecerFechaAsistencia(LocalDateTime fechaAsistencia);

    MedicoId getMedicoId();

    LocalDateTime getFechaHora();
}
