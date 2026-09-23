package com.piedrazul.citas.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteCreadoEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private PacienteData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacienteData {
        private Long pacienteId;
        private String nombreCompleto;
        private String email;
        private String telefono;
        private boolean activo;
    }
}