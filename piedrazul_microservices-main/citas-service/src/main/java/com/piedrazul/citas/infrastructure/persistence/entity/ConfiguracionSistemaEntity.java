package com.piedrazul.citas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
public class ConfiguracionSistemaEntity {

    @Id
    private Long id = 1L;

    private Integer semanasDisponibles;

    @Column(columnDefinition = "TEXT")
    private String festivos;
}
