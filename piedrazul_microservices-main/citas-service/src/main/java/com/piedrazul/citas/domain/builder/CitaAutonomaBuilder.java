package com.piedrazul.citas.domain.builder;

import com.piedrazul.citas.domain.exception.RestriccionAutoservicioException;
import com.piedrazul.citas.domain.model.*;

import java.time.LocalDateTime;

public class CitaAutonomaBuilder extends CitaBuilder {

    @Override
    public Cita build() {

        validarPacienteActivo();
        validarMedicoActivo();
        validarEspecialidad();
        validarMedicoAtiendeEspecialidad();
        validarDisponibilidad();

        validarRestriccionesAutoservicio();

        return construir();
    }

    private void validarRestriccionesAutoservicio() {

        if (fechaHora.isBefore(LocalDateTime.now().plusHours(24))) {

            throw new RestriccionAutoservicioException(
                    "Las citas autónomas requieren 24h de anticipación"
            );
        }
    }
}