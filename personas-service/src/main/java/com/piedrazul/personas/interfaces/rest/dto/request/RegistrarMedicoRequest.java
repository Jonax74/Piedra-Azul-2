package com.piedrazul.personas.interfaces.rest.dto.request;

import com.piedrazul.personas.domain.model.EspecialidadMedica;
import com.piedrazul.personas.domain.model.TipoProfesional;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RegistrarMedicoRequest {

    @NotNull
    private Long personaId;

    @NotNull
    private TipoProfesional tipoProfesional;

    @NotEmpty(message = "Debe indicar al menos una especialidad")
    private Set<EspecialidadMedica> especialidades;
}