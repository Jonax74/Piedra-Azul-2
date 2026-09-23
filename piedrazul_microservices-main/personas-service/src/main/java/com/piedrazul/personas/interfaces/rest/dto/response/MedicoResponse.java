package com.piedrazul.personas.interfaces.rest.dto.response;

import com.piedrazul.personas.domain.model.EspecialidadMedica;
import com.piedrazul.personas.domain.model.EstadoMedico;
import com.piedrazul.personas.domain.model.TipoProfesional;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class MedicoResponse {

    private Long personaId;

    private String primerNombre;
    private String primerApellido;

    private TipoProfesional tipoProfesional;
    private EstadoMedico estado;
    private Set<EspecialidadMedica> especialidades;
}