package com.piedrazul.citas.application.mapper;

import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.model.MedicoSnapshot;
import com.piedrazul.citas.domain.model.PacienteSnapshot;
import org.springframework.stereotype.Component;

@Component
public class CitaApplicationMapper {

    public CitaResponse toResponse(Cita cita, PacienteSnapshot paciente, MedicoSnapshot medico) {
        if (cita == null) return null;

        return CitaResponse.builder()
                .id(cita.getId() != null ? cita.getId().toString() : null)
                .pacienteId(cita.getPacienteId() != null ? cita.getPacienteId().value() : null)
                .pacienteNombre(paciente != null ? paciente.getNombreCompleto() : null)
                .medicoId(cita.getMedicoId() != null ? cita.getMedicoId().value() : null)
                .medicoNombre(medico != null ? medico.getNombreCompleto() : null)
                .especialidad(cita.getEspecialidad())
                .fechaHora(cita.getFechaHora())
                .estado(cita.getEstado() != null ? cita.getEstado().getDescripcion() : null)
                .motivoAgendamiento(cita.getMotivoAgendamiento())
                .motivoCancelacion(cita.getMotivoCancelacion())
                .fechaAsistencia(cita.getFechaAsistencia())
                .createdAt(cita.getAudit() != null ? cita.getAudit().getCreatedAt() : null)
                .build();
    }
}