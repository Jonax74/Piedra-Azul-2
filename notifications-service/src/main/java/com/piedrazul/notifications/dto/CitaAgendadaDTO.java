package com.piedrazul.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaAgendadaDTO {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private CitaData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitaData {
        private String citaId;
        private Long pacienteId;
        private String pacienteNombre;
        private String pacienteEmail;
        private Long medicoId;
        private String medicoNombre;
        private String medicoEmail;
        private LocalDateTime fechaHora;
        private String estado;
    }
}