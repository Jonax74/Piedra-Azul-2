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
public class CitaReagendadaDTO {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private CitaReagendadaData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitaReagendadaData {
        private String citaId;
        private Long pacienteId;
        private String pacienteEmail;
        private Long medicoId;
        private String medicoEmail;
        private LocalDateTime fechaHoraOriginal;
        private LocalDateTime nuevaFechaHora;
    }
}
