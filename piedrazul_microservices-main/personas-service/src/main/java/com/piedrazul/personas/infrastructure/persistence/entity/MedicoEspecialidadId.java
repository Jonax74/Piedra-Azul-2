package com.piedrazul.personas.infrastructure.persistence.entity;

import com.piedrazul.personas.domain.model.EspecialidadMedica;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicoEspecialidadId implements Serializable {

    @Column(name = "medico_id")
    private Long medicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "especialidad", length = 30)
    private EspecialidadMedica especialidad;
}
