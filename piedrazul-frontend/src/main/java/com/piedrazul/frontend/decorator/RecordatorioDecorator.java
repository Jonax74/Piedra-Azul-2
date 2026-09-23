package com.piedrazul.frontend.decorator;

public class RecordatorioDecorator extends CitaDecorator {

    public RecordatorioDecorator(CitaComponent wrapped) {
        super(wrapped);
    }

    @Override
    public String getDescripcion() {
        return wrapped.getDescripcion() + " 🔔 (recordatorio en 1 hora)";
    }

    @Override
    public String getColor() {
        return "#FFD93D"; // Amarillo
    }
}