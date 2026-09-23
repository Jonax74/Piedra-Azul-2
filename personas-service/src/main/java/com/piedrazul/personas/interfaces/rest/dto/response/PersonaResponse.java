package com.piedrazul.personas.interfaces.rest.dto.response;

import com.piedrazul.personas.domain.model.Genero;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PersonaResponse {

    private Long id;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Genero genero;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String dni;
    private String correo;
}