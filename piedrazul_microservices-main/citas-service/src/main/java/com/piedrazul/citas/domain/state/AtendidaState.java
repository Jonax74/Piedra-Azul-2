package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;

final class AtendidaState extends AbstractEstadoCitaState {

    static final AtendidaState INSTANCE = new AtendidaState();

    private AtendidaState() {
        super(EstadoCita.ATENDIDA);
    }

    @Override
    public boolean esFinal() {
        return false;
    }

    @Override
    public boolean puedeReagendarCreandoNuevaCita() {
        return true;
    }
}
