package com.piedrazul.citas.infrastructure.persistence.entity;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "medicos_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoSnapshotEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column
    private String email;

    @Column
    private String especialidad;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "medico_snapshot_especialidad",
            joinColumns = @JoinColumn(name = "medico_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "especialidad", nullable = false, length = 30)
    @Builder.Default
    private Set<EspecialidadMedica> especialidades = new LinkedHashSet<>();

    @Column(nullable = false)
    private String estado;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
        if (especialidades != null && !especialidades.isEmpty()) {
            especialidad = especialidades.stream()
                    .map(Enum::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(null);
        }
    }
}
