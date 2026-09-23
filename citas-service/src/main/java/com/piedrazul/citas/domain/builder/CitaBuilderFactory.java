package com.piedrazul.citas.domain.builder;

import com.piedrazul.citas.domain.model.TipoAgendamiento;

public class CitaBuilderFactory {

    private CitaBuilderFactory() {}

    public static CitaBuilder crear(TipoAgendamiento tipo) {

        return switch (tipo) {

            case AUTONOMO -> new CitaAutonomaBuilder();

            case MANUAL -> new CitaManualBuilder();

            case REAGENDAMIENTO -> null; // Implementar posteriormente reagendamiento
        };
    }
}
