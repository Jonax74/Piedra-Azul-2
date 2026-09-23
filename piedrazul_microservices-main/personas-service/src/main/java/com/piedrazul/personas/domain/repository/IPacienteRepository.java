package com.piedrazul.personas.domain.repository;

import com.piedrazul.personas.domain.model.Paciente;
import java.util.List;
import java.util.Optional;

public interface IPacienteRepository {

    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorPersonaId(Long personaId);

    boolean existsByPersonaId(Long personaId);

    List<Paciente> listarTodos();
}