package com.piedrazul.personas.infrastructure.persistence.entity;

import com.piedrazul.personas.domain.model.EstadoMedico;
import com.piedrazul.personas.domain.model.TipoProfesional;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicoEntity {

    @Id
    @Column(name = "per_id")
    private Long personaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "per_id")
    private PersonaEntity persona;

    @Enumerated(EnumType.STRING)
    @Column(name = "med_tipo_profesional", nullable = false, length = 20)
    private TipoProfesional tipoProfesional;

    @Enumerated(EnumType.STRING)
    @Column(name = "med_estado", nullable = false, length = 20)
    private EstadoMedico estado;

    @Column(name = "intervalo_minutos")
    private Integer intervaloMinutos;
}