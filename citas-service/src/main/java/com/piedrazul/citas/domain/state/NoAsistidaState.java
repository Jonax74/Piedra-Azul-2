package com.piedrazul.citas.domain.state;

import com.piedrazul.citas.domain.model.EstadoCita;

final class NoAsistidaState extends AbstractEstadoCitaState {

    static final NoAsistidaState INSTANCE = new NoAsistidaState();

    private NoAsistidaState() {
        super(EstadoCita.NO_ASISTIDA);
    }

    @Override
    public boolean esFinal() {
        return true;
    }
}
