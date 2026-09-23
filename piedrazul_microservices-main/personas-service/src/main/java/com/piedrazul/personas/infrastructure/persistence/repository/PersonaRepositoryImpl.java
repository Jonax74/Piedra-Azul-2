package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.infrastructure.persistence.entity.PersonaEntity;
import com.piedrazul.personas.infrastructure.persistence.mapper.PersonaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonaRepositoryImpl implements IPersonaRepository {

    private final SpringDataPersonaRepository springDataPersonaRepository;
    private final PersonaMapper personaMapper;

    @Override
    public Persona guardar(Persona persona) {
        PersonaEntity entity = personaMapper.toEntity(persona);
        PersonaEntity saved = springDataPersonaRepository.save(entity);
        return personaMapper.toDomain(saved);
    }

    @Override
    public Optional<Persona> buscarPorId(Long id) {
        return springDataPersonaRepository.findById(id)
                .map(personaMapper::toDomain);
    }

    @Override
    public Optional<Persona> buscarPorDni(String dni) {
        return springDataPersonaRepository.findByDni(dni)
                .map(personaMapper::toDomain);
    }

    @Override
    public boolean existePorDni(String dni) {
        return springDataPersonaRepository.existsByDni(dni);
    }

    @Override
    public List<Persona> listarTodos() {
        return springDataPersonaRepository.findAll()
                .stream()
                .map(personaMapper::toDomain)
                .toList();
    }
}