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
public class CitaCanceladaEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private CitaCanceladaData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitaCanceladaData {
        private String citaId;
        private Long pacienteId;
        private String pacienteEmail;
        private Long medicoId;
        private String medicoEmail;
        private LocalDateTime fechaHoraOriginal;
        private String motivo;
        private LocalDateTime fechaCancelacion;
    }

    public static CitaCanceladaEvent create(String citaId, Long pacienteId, String pacienteEmail,
                                            Long medicoId, String medicoEmail,
                                            LocalDateTime fechaHoraOriginal, String motivo) {
        return CitaCanceladaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CITA_CANCELADA")
                .timestamp(LocalDateTime.now())
                .data(CitaCanceladaData.builder()
                        .citaId(citaId)
                        .pacienteId(pacienteId)
                        .pacienteEmail(pacienteEmail)
                        .medicoId(medicoId)
                        .medicoEmail(medicoEmail)
                        .fechaHoraOriginal(fechaHoraOriginal)
                        .motivo(motivo)
                        .fechaCancelacion(LocalDateTime.now())
                        .build())
                .build();
    }
}