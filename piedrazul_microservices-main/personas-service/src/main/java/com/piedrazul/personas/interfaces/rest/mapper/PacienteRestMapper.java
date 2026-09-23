package com.piedrazul.personas.interfaces.rest.mapper;

import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.interfaces.rest.dto.response.PacienteResponse;
import org.springframework.stereotype.Component;

@Component
public class PacienteRestMapper {

    public PacienteResponse toResponse(Paciente paciente) {
        return PacienteResponse.builder()
                .personaId(paciente.getPersonaId())
                .build();
    }
}