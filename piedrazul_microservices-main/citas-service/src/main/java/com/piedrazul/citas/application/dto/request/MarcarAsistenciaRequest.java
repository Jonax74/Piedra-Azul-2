package com.piedrazul.citas.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcarAsistenciaRequest {
    private String citaId;
    private boolean asistio; // true: atendida, false: no asistida
}