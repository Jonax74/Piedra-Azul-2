package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.domain.repository.IPacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarPacienteService {

    private final IPacienteRepository pacienteRepository;

    public Paciente buscarPorPersonaId(Long personaId) {
        return pacienteRepository.buscarPorPersonaId(personaId)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe un paciente para la personaId: " + personaId
                ));
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.listarTodos();
    }
}