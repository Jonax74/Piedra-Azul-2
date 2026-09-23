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
public class DisponibilidadModificadaEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private DisponibilidadModificadaData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisponibilidadModificadaData {
        private Long medicoId;
        private String diaSemanaAnterior;
        private LocalTime horaInicioAnterior;
        private LocalTime horaFinAnterior;
        private Long medicoIdNuevo;
        private String diaSemanaNuevo;
        private LocalTime horaInicioNuevo;
        private LocalTime horaFinNuevo;
        private Integer intervaloMinutos;
    }
}
