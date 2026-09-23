package com.piedrazul.personas.infrastructure.persistence.entity;

import com.piedrazul.personas.domain.model.Genero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "persona")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "per_id")
    private Long id;

    @Column(name = "per_primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    @Column(name = "per_segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "per_primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Column(name = "per_segundo_apellido", length = 50)
    private String segundoApellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "per_genero", nullable = false, length = 10)
    private Genero genero;

    @Column(name = "per_fecha_nac")
    private LocalDate fechaNacimiento;

    @Column(name = "per_telefono", nullable = false, length = 15)
    private String telefono;

    @Column(name = "per_dni", nullable = false, unique = true, length = 30)
    private String dni;

    @Column(name = "per_correo", length = 70)
    private String correo;
}