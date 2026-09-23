package com.piedrazul.frontend.decorator;

public abstract class CitaDecorator implements CitaComponent {
    protected CitaComponent wrapped;

    public CitaDecorator(CitaComponent wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getDescripcion() {
        return wrapped.getDescripcion();
    }

    @Override
    public String getColor() {
        return wrapped.getColor();
    }

    @Override
    public com.piedrazul.frontend.dto.response.CitaResponse getCita() {
        return wrapped.getCita();
    }
}