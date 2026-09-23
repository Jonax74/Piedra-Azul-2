package com.piedrazul.personas.infrastructure.persistence.mapper;

import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.infrastructure.persistence.entity.PacienteEntity;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

    public Paciente toDomain(PacienteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Paciente(entity.getPersonaId());
    }
}