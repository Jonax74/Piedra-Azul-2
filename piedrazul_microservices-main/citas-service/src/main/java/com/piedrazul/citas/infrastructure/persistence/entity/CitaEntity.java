package com.piedrazul.citas.infrastructure.persistence.entity;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "citas",
        indexes = {
                @Index(name = "idx_medico_fecha", columnList = "medico_id, fecha_hora"),
                @Index(name = "idx_paciente_fecha", columnList = "paciente_id, fecha_hora")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_medico_fecha", columnNames = {"medico_id", "fecha_hora"}),
                @UniqueConstraint(name = "uk_paciente_fecha", columnNames = {"paciente_id", "fecha_hora"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;  // ← Cambiar de String a UUID

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "especialidad", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'GENERAL'")
    private EspecialidadMedica especialidad;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "motivo_agendamiento", length = 500)
    private String motivoAgendamiento;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "fecha_asistencia")
    private LocalDateTime fechaAsistencia;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}