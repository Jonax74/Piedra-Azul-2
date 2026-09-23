package com.piedrazul.citas.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelarCitaRestRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    @NotBlank(message = "El ID de la cita no puede estar vacío")
    private String citaId;

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String motivo;
}