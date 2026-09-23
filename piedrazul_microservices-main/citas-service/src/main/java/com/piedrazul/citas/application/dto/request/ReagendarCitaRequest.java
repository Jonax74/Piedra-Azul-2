package com.piedrazul.citas.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReagendarCitaRequest {
    private String citaId;
    private LocalDateTime nuevaFechaHora;
}