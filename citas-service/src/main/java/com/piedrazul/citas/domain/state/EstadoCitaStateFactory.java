package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;

public final class EstadoCitaStateFactory {

    private EstadoCitaStateFactory() {
    }

    public static EstadoCitaState of(EstadoCita estado) {
        if (estado == null) {
            return ProgramadaState.INSTANCE;
        }
        return switch (estado) {
            case PROGRAMADA -> ProgramadaState.INSTANCE;
            case REAGENDADA -> ReagendadaState.INSTANCE;
            case ATENDIDA -> AtendidaState.INSTANCE;
            case CANCELADA -> CanceladaState.INSTANCE;
            case NO_ASISTIDA -> NoAsistidaState.INSTANCE;
        };
    }
}
