package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.EstadoCita;

import java.time.LocalDateTime;

/**
 * Estado concreto del patrón State (GoF) para el ciclo de vida de una cita.
 */
public interface EstadoCitaState {

    EstadoCita getTipo();

    boolean esFinal();

    boolean puedeCancelar();

    boolean puedeReagendarEnMismaCita();

    boolean puedeReagendarCreandoNuevaCita();

    boolean puedeMarcarComoAtendida(LocalDateTime fechaCita);

    boolean puedeMarcarComoNoAsistida(LocalDateTime fechaCita);

    void cancelar(EstadoCitaContext contexto, String motivo);

    void reagendarEnMismaCita(
            EstadoCitaContext contexto,
            LocalDateTime nuevaFechaHora,
            DisponibilidadSnapshot disponibilidad
    );

    void marcarComoAtendida(EstadoCitaContext contexto);

    void marcarComoNoAsistida(EstadoCitaContext contexto);
}
