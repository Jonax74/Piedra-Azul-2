package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.EstadoMedico;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CambiarEstadoMedicoService {

    private final IMedicoRepository medicoRepository;

    @Transactional
    public Medico ejecutar(Long personaId, EstadoMedico nuevoEstado) {

        Medico medico = medicoRepository.buscarPorPersonaId(personaId)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe un médico con personaId: " + personaId
                ));

        medico.cambiarEstado(nuevoEstado);

        return medicoRepository.guardar(medico);
    }
}