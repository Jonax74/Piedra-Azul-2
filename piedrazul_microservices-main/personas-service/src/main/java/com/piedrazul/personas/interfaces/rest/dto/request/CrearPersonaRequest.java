package com.piedrazul.personas.interfaces.rest.dto.request;

import com.piedrazul.personas.domain.model.Genero;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CrearPersonaRequest {

    @NotBlank
    private String primerNombre;

    private String segundoNombre;

    @NotBlank
    private String primerApellido;

    private String segundoApellido;

    @NotNull
    private Genero genero;

    @PastOrPresent
    private LocalDate fechaNacimiento;

    @NotBlank
    private String telefono;

    @NotBlank
    private String dni;

    @Email
    private String correo;
}