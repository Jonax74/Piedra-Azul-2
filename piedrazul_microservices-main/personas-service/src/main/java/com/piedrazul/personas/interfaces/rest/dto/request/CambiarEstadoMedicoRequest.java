package com.piedrazul.personas.interfaces.rest.dto.request;

import com.piedrazul.personas.domain.model.EstadoMedico;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarEstadoMedicoRequest {

    @NotNull
    private EstadoMedico estado;
}