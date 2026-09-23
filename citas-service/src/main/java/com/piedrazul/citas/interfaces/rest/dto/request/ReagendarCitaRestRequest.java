package com.piedrazul.citas.interfaces.rest.dto.request;

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
public class ReagendarCitaRestRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private String citaId;

    @NotNull(message = "La nueva fecha y hora son obligatorias")
    private LocalDateTime nuevaFechaHora;
}