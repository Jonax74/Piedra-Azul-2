package com.piedrazul.personas.infrastructure.persistence.mapper;

import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.infrastructure.persistence.entity.DisponibilidadEntity;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadMapper {

    public DisponibilidadEntity toEntity(Disponibilidad domain) {
        if (domain == null) return null;

        return DisponibilidadEntity.builder()
                .id(domain.getId())
                .medicoId(domain.getMedicoId())
                .diaSemana(domain.getDiaSemana())
                .horaInicio(domain.getHoraInicio())
                .horaFin(domain.getHoraFin())
                .intervaloMinutos(domain.getIntervaloMinutos())
                .build();
    }

    public Disponibilidad toDomain(DisponibilidadEntity entity) {
        if (entity == null) return null;

        Disponibilidad domain = new Disponibilidad();
        domain.setId(entity.getId());
        domain.setMedicoId(entity.getMedicoId());
        domain.setDiaSemana(entity.getDiaSemana());
        domain.setHoraInicio(entity.getHoraInicio());
        domain.setHoraFin(entity.getHoraFin());
        domain.setIntervaloMinutos(entity.getIntervaloMinutos());
        return domain;
    }
}