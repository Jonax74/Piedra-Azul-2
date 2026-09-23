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
public class CitaReagendadaEvent {
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

    public static CitaReagendadaEvent create(String citaId, Long pacienteId, String pacienteEmail,
                                             Long medicoId, String medicoEmail,
                                             LocalDateTime fechaHoraOriginal,
                                             LocalDateTime nuevaFechaHora) {
        return CitaReagendadaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CITA_REAGENDADA")
                .timestamp(LocalDateTime.now())
                .data(CitaReagendadaData.builder()
                        .citaId(citaId)
                        .pacienteId(pacienteId)
                        .pacienteEmail(pacienteEmail)
                        .medicoId(medicoId)
                        .medicoEmail(medicoEmail)
                        .fechaHoraOriginal(fechaHoraOriginal)
                        .nuevaFechaHora(nuevaFechaHora)
                        .build())
                .build();
    }
}