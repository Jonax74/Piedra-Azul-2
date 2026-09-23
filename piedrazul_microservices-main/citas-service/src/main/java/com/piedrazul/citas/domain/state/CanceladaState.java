package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;

final class CanceladaState extends AbstractEstadoCitaState {

    static final CanceladaState INSTANCE = new CanceladaState();

    private CanceladaState() {
        super(EstadoCita.CANCELADA);
    }

    @Override
    public boolean esFinal() {
        return true;
    }
}
