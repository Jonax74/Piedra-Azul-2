package com.piedrazul.personas.infrastructure.persistence.mapper;

import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicoMapper {

    public Medico toDomain(MedicoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Medico(
                entity.getPersonaId(),
                entity.getTipoProfesional(),
                entity.getEstado()
        );
    }
}