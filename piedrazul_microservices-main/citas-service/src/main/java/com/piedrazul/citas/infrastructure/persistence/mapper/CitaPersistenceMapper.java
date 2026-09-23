package com.piedrazul.citas.infrastructure.persistence.mapper;

import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.model.EspecialidadMedica;
import com.piedrazul.citas.domain.model.EstadoCita;
import com.piedrazul.citas.domain.valueobjects.*;
import com.piedrazul.citas.infrastructure.persistence.entity.CitaEntity;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CitaPersistenceMapper {

    public CitaEntity toEntity(Cita cita) {
        if (cita == null) return null;

        return CitaEntity.builder()
                .id(UUID.fromString(cita.getId().toString()))
                .pacienteId(cita.getPacienteId().value())
                .medicoId(cita.getMedicoId().value())
                .especialidad(cita.getEspecialidad())
                .creadoPor(cita.getCreadoPor().value())
                .fechaHora(cita.getFechaHora())
                .estado(cita.getEstado().name())
                .motivoAgendamiento(cita.getMotivoAgendamiento())
                .motivoCancelacion(cita.getMotivoCancelacion())
                .fechaAsistencia(cita.getFechaAsistencia())
                .createdAt(cita.getAudit().getCreatedAt())
                .updatedAt(cita.getAudit().getUpdatedAt())
                .createdBy(cita.getAudit().getCreatedBy())
                .build();
    }

    public Cita toDomain(CitaEntity entity) {
        if (entity == null) return null;

        return Cita.reconstruir(
                CitaId.fromString(entity.getId().toString()),
                PacienteId.of(entity.getPacienteId()),
                MedicoId.of(entity.getMedicoId()),
                entity.getEspecialidad() != null
                        ? entity.getEspecialidad()
                        : EspecialidadMedica.GENERAL,
                UsuarioId.of(entity.getCreadoPor()),
                entity.getFechaHora(),
                EstadoCita.fromPersisted(entity.getEstado()),
                entity.getMotivoAgendamiento(),
                entity.getMotivoCancelacion(),
                entity.getFechaAsistencia(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }
}