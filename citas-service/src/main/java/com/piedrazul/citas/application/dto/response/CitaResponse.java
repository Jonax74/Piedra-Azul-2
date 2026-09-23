package com.piedrazul.citas.application.dto.response;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponse {
    private String id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long medicoId;
    private String medicoNombre;
    private EspecialidadMedica especialidad;
    private LocalDateTime fechaHora;
    private String estado;
    private String motivoAgendamiento;
    private String motivoCancelacion;
    private LocalDateTime fechaAsistencia;
    private LocalDateTime createdAt;
}