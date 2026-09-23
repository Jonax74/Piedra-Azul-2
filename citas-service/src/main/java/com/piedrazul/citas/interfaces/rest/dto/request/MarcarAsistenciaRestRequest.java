package com.piedrazul.citas.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcarAsistenciaRestRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private String citaId;

    @NotNull(message = "El campo asistio es obligatorio")
    private Boolean asistio;
}