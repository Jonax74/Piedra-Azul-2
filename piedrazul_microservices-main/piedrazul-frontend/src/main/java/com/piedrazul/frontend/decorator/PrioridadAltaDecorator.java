package com.piedrazul.frontend.decorator;

public class PrioridadAltaDecorator extends CitaDecorator {

    public PrioridadAltaDecorator(CitaComponent wrapped) {
        super(wrapped);
    }

    @Override
    public String getDescripcion() {
        return "⚠️ URGENTE - " + wrapped.getDescripcion();
    }

    @Override
    public String getColor() {
        return "#FF6B6B"; // Rojo
    }
}