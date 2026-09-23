package com.piedrazul.personas.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrarPacienteRequest {

    @NotNull
    private Long personaId;
}