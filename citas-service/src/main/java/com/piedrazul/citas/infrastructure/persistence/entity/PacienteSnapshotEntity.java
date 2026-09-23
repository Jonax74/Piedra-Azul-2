package com.piedrazul.citas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "pacientes_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteSnapshotEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}