package com.piedrazul.citas.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ValidarModificacionDisponibilidadRequest {

    @NotNull
    private Long medicoIdActual;

    @NotNull
    private String diaSemanaActual;

    @NotNull
    private LocalTime horaInicioActual;

    @NotNull
    private LocalTime horaFinActual;

    @NotNull
    private Long medicoIdNuevo;

    @NotNull
    private String diaSemanaNuevo;

    @NotNull
    private LocalTime horaInicioNuevo;

    @NotNull
    private LocalTime horaFinNuevo;
}
