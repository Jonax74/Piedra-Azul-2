package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.infrastructure.persistence.repository.SpringDataMedicoRepository;
import com.piedrazul.personas.infrastructure.persistence.repository.SpringDataPacienteRepository;
import com.piedrazul.personas.infrastructure.persistence.repository.SpringDataPersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompensarRegistroFallidoService {

    private final SpringDataPacienteRepository pacienteRepository;
    private final SpringDataMedicoRepository medicoRepository;
    private final SpringDataPersonaRepository personaRepository;

    @Transactional
    public void ejecutar(Long personaId) {
        if (!personaRepository.existsById(personaId)) {
            throw new PersonaNoEncontradaException("Persona con id " + personaId + " no encontrada");
        }

        if (pacienteRepository.existsById(personaId)) {
            pacienteRepository.deleteById(personaId);
        }

        if (medicoRepository.existsById(personaId)) {
            medicoRepository.deleteById(personaId);
        }

        personaRepository.deleteById(personaId);
    }
}
