package com.piedrazul.citas.application.dto.request;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearCitaRequest {
    private Long pacienteId;
    private Long medicoId;
    private Long usuarioCreadorId;
    private LocalDateTime fechaHora;

    @NotNull(message = "La especialidad es obligatoria")
    private EspecialidadMedica especialidad;

    private String motivoAgendamiento;
}