package com.piedrazul.notifications.dto;

import com.piedrazul.notifications.domain.model.TipoNotificacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificacionResponse {
    private String id;
    private Long personaId;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private String citaId;
    private boolean leida;
    private LocalDateTime fechaCreacion;
}
