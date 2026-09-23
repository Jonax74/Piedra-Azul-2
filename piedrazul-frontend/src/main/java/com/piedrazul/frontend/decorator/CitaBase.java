package com.piedrazul.frontend.decorator;

import com.piedrazul.frontend.dto.response.CitaResponse;

public class CitaBase implements CitaComponent {
    protected CitaResponse cita;

    public CitaBase(CitaResponse cita) {
        this.cita = cita;
    }

    @Override
    public String getDescripcion() {
        return cita.getFechaHora() + " - " + cita.getMedicoNombre();
    }

    @Override
    public String getColor() {
        return "#FFFFFF"; // Blanco (normal)
    }

    @Override
    public CitaResponse getCita() {
        return cita;
    }
}