package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaYaExisteException;
import com.piedrazul.personas.domain.model.Genero;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CrearPersonaService {

    private final IPersonaRepository personaRepository;

    @Transactional
    public Persona ejecutar(
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            Genero genero,
            LocalDate fechaNacimiento,
            String telefono,
            String dni,
            String correo
    ) {
        if (personaRepository.existePorDni(dni)) {
            throw new PersonaYaExisteException(
                    "Ya existe una persona registrada con el DNI: " + dni
            );
        }

        Persona persona = Persona.crear(
                primerNombre,
                segundoNombre,
                primerApellido,
                segundoApellido,
                genero,
                fechaNacimiento,
                telefono,
                dni,
                correo
        );

        return personaRepository.guardar(persona);
    }
}