package com.piedrazul.personas.infrastructure.persistence.mapper;

import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.infrastructure.persistence.entity.PersonaEntity;
import org.springframework.stereotype.Component;

@Component
public class PersonaMapper {

    public PersonaEntity toEntity(Persona persona) {
        if (persona == null) {
            return null;
        }

        PersonaEntity entity = new PersonaEntity();
        entity.setId(persona.getId());
        entity.setPrimerNombre(persona.getPrimerNombre());
        entity.setSegundoNombre(persona.getSegundoNombre());
        entity.setPrimerApellido(persona.getPrimerApellido());
        entity.setSegundoApellido(persona.getSegundoApellido());
        entity.setGenero(persona.getGenero());
        entity.setFechaNacimiento(persona.getFechaNacimiento());
        entity.setTelefono(persona.getTelefono());
        entity.setDni(persona.getDni());
        entity.setCorreo(persona.getCorreo());
        return entity;
    }

    public Persona toDomain(PersonaEntity entity) {
        if (entity == null) {
            return null;
        }

        Persona persona = new Persona(
                entity.getId(),
                entity.getPrimerNombre(),
                entity.getSegundoNombre(),
                entity.getPrimerApellido(),
                entity.getSegundoApellido(),
                entity.getGenero(),
                entity.getFechaNacimiento(),
                entity.getTelefono(),
                entity.getDni(),
                entity.getCorreo()
        );

        return persona;
    }
}