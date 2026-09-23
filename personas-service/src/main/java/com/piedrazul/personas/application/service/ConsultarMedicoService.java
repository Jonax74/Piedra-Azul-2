package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarMedicoService {

    private final IMedicoRepository medicoRepository;

    public Medico buscarPorPersonaId(Long personaId) {
        return medicoRepository.buscarPorPersonaId(personaId)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe un médico para la personaId: " + personaId
                ));
    }

    public List<Medico> listarTodos() {
        return medicoRepository.listarTodos();
    }

    public List<Medico> listarActivos() {
        return medicoRepository.listarActivos();
    }
}