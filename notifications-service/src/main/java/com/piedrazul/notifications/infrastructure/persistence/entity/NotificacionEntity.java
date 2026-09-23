package com.piedrazul.notifications.infrastructure.persistence.entity;

import com.piedrazul.notifications.domain.model.TipoNotificacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacion", indexes = {
        @Index(name = "idx_notificacion_persona_leida", columnList = "persona_id, leida"),
        @Index(name = "idx_notificacion_persona_fecha", columnList = "persona_id, fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificacionEntity {

    @Id
    @Column(name = "not_id", length = 36)
    private String id;

    @Column(name = "persona_id", nullable = false)
    private Long personaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "cita_id", length = 36)
    private String citaId;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public static NotificacionEntity crear(Long personaId,
                                           TipoNotificacion tipo,
                                           String titulo,
                                           String mensaje,
                                           String citaId) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.id = UUID.randomUUID().toString();
        entity.personaId = personaId;
        entity.tipo = tipo;
        entity.titulo = titulo;
        entity.mensaje = mensaje;
        entity.citaId = citaId;
        entity.leida = false;
        entity.fechaCreacion = LocalDateTime.now();
        return entity;
    }
}
