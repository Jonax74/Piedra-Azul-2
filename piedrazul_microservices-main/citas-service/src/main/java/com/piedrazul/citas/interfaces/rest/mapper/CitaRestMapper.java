package com.piedrazul.citas.interfaces.rest.mapper;

import com.piedrazul.citas.application.dto.request.CancelarCitaRequest;
import com.piedrazul.citas.application.dto.request.CrearCitaRequest;
import com.piedrazul.citas.application.dto.request.MarcarAsistenciaRequest;
import com.piedrazul.citas.application.dto.request.ReagendarCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.interfaces.rest.dto.request.CancelarCitaRestRequest;
import com.piedrazul.citas.interfaces.rest.dto.request.CrearCitaRestRequest;
import com.piedrazul.citas.interfaces.rest.dto.request.MarcarAsistenciaRestRequest;
import com.piedrazul.citas.interfaces.rest.dto.request.ReagendarCitaRestRequest;
import com.piedrazul.citas.interfaces.rest.dto.response.CitaRestResponse;
import org.springframework.stereotype.Component;

@Component
public class CitaRestMapper {

    // Convertir de REST Request a Application Request
    public CrearCitaRequest toApplicationRequest(CrearCitaRestRequest request) {
        if (request == null) return null;

        return CrearCitaRequest.builder()
                .pacienteId(request.getPacienteId())
                .medicoId(request.getMedicoId())
                .usuarioCreadorId(request.getUsuarioCreadorId())
                .fechaHora(request.getFechaHora())
                .especialidad(request.getEspecialidad())
                .motivoAgendamiento(request.getMotivoAgendamiento())
                .build();
    }

    public CancelarCitaRequest toApplicationRequest(CancelarCitaRestRequest request) {
        if (request == null) return null;

        return CancelarCitaRequest.builder()
                .citaId(request.getCitaId())
                .motivo(request.getMotivo())
                .build();
    }

    public ReagendarCitaRequest toApplicationRequest(ReagendarCitaRestRequest request) {
        if (request == null) return null;

        return ReagendarCitaRequest.builder()
                .citaId(request.getCitaId())
                .nuevaFechaHora(request.getNuevaFechaHora())
                .build();
    }

    public MarcarAsistenciaRequest toApplicationRequest(MarcarAsistenciaRestRequest request) {
        if (request == null) return null;

        return MarcarAsistenciaRequest.builder()
                .citaId(request.getCitaId())
                .asistio(request.getAsistio())
                .build();
    }

    // Convertir de Application Response a REST Response
    public CitaRestResponse toRestResponse(CitaResponse response) {
        if (response == null) return null;

        return CitaRestResponse.builder()
                .id(response.getId())
                .pacienteId(response.getPacienteId())
                .pacienteNombre(response.getPacienteNombre())
                .medicoId(response.getMedicoId())
                .medicoNombre(response.getMedicoNombre())
                .especialidad(response.getEspecialidad())
                .fechaHora(response.getFechaHora())
                .estado(response.getEstado())
                .motivoAgendamiento(response.getMotivoAgendamiento())
                .motivoCancelacion(response.getMotivoCancelacion())
                .fechaAsistencia(response.getFechaAsistencia())
                .createdAt(response.getCreatedAt())
                .build();
    }
}