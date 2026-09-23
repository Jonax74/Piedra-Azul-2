package com.piedrazul.citas.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaAgendadaEvent {
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

    public static CitaAgendadaEvent create(String citaId, Long pacienteId, String pacienteNombre,
                                           String pacienteEmail, Long medicoId, String medicoNombre,
                                           String medicoEmail, LocalDateTime fechaHora, String estado) {
        return CitaAgendadaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CITA_AGENDADA")
                .timestamp(LocalDateTime.now())
                .data(CitaData.builder()
                        .citaId(citaId)
                        .pacienteId(pacienteId)
                        .pacienteNombre(pacienteNombre)
                        .pacienteEmail(pacienteEmail)
                        .medicoId(medicoId)
                        .medicoNombre(medicoNombre)
                        .medicoEmail(medicoEmail)
                        .fechaHora(fechaHora)
                        .estado(estado)
                        .build())
                .build();
    }
}