package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarPersonaService {

    private final IPersonaRepository personaRepository;

    public Persona buscarPorId(Long id) {
        return personaRepository.buscarPorId(id)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe una persona con id: " + id
                ));
    }

    public Persona buscarPorDni(String dni) {
        return personaRepository.buscarPorDni(dni)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe una persona con DNI: " + dni
                ));
    }

    public List<Persona> listarTodos() {
        return personaRepository.listarTodos();
    }
}