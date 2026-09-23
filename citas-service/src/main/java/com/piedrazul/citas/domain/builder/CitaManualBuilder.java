package com.piedrazul.citas.domain.builder;

import com.piedrazul.citas.domain.model.Cita;

public class CitaManualBuilder extends CitaBuilder {

    @Override
    public Cita build() {

        validarPacienteActivo();
        validarMedicoActivo();
        validarEspecialidad();
        validarMedicoAtiendeEspecialidad();
        validarDisponibilidad();

        return construir();
    }
}
