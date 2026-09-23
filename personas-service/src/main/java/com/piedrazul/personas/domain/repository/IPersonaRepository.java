package com.piedrazul.personas.domain.repository;

import com.piedrazul.personas.domain.model.Persona;

import java.util.List;
import java.util.Optional;

public interface IPersonaRepository {

    Persona guardar(Persona persona);

    Optional<Persona> buscarPorId(Long id);

    Optional<Persona> buscarPorDni(String dni);

    boolean existePorDni(String dni);

    List<Persona> listarTodos();
}