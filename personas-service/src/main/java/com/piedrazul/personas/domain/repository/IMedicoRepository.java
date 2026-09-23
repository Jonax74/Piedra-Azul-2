package com.piedrazul.personas.domain.repository;

import com.piedrazul.personas.domain.model.Medico;

import java.util.List;
import java.util.Optional;

public interface IMedicoRepository {

    Medico guardar(Medico medico);

    Optional<Medico> buscarPorPersonaId(Long personaId);

    boolean existePorPersonaId(Long personaId);

    List<Medico> listarTodos();

    List<Medico> listarActivos();
}