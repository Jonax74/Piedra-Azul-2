package com.piedrazul.personas.interfaces.rest.mapper;

import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.interfaces.rest.dto.response.PersonaResponse;
import org.springframework.stereotype.Component;

@Component
public class PersonaRestMapper {

    public PersonaResponse toResponse(Persona persona) {
        return PersonaResponse.builder()
                .id(persona.getId())
                .primerNombre(persona.getPrimerNombre())
                .segundoNombre(persona.getSegundoNombre())
                .primerApellido(persona.getPrimerApellido())
                .segundoApellido(persona.getSegundoApellido())
                .genero(persona.getGenero())
                .fechaNacimiento(persona.getFechaNacimiento())
                .telefono(persona.getTelefono())
                .dni(persona.getDni())
                .correo(persona.getCorreo())
                .build();
    }
}