package com.piedrazul.personas.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadActualizadaEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private DisponibilidadData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisponibilidadData {
        private Long medicoId;
        private Map<String, List<Map<String, String>>> horariosSemanales;
        private List<LocalDateTime> bloqueosEspecificos;
        private Integer intervaloMinutos;
    }
}