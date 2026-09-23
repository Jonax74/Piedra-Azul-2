package com.piedrazul.personas.interfaces.rest.dto.request;

import com.piedrazul.personas.domain.model.EspecialidadMedica;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AsignarEspecialidadesMedicoRequest {

    @NotEmpty(message = "Debe indicar al menos una especialidad")
    private Set<EspecialidadMedica> especialidades;
}
