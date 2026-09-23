package com.piedrazul.personas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medico_especialidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicoEspecialidadEntity {

    @EmbeddedId
    private MedicoEspecialidadId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("medicoId")
    @JoinColumn(name = "medico_id")
    private MedicoEntity medico;
}
