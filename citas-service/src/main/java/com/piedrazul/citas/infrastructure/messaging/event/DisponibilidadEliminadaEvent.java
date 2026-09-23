package com.piedrazul.citas.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadEliminadaEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private DisponibilidadEliminadaData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisponibilidadEliminadaData {
        private Long medicoId;
        private String diaSemana;
        private LocalTime horaInicio;
        private LocalTime horaFin;
    }
}
