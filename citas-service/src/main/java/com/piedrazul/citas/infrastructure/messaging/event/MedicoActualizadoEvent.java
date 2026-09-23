package com.piedrazul.citas.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoActualizadoEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private MedicoActualizadoData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicoActualizadoData {
        private Long medicoId;
        private String nombreCompleto;
        private String email;
        private String especialidad;
        private List<String> especialidades;
        private String estado;
    }
}