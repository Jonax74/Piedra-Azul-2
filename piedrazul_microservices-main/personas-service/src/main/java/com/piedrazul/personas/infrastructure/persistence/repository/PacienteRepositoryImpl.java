package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.domain.repository.IPacienteRepository;
import com.piedrazul.personas.infrastructure.persistence.entity.PacienteEntity;
import com.piedrazul.personas.infrastructure.persistence.entity.PersonaEntity;
import com.piedrazul.personas.infrastructure.persistence.mapper.PacienteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PacienteRepositoryImpl implements IPacienteRepository {

    private final SpringDataPacienteRepository springDataPacienteRepository;
    private final SpringDataPersonaRepository springDataPersonaRepository;
    private final PacienteMapper pacienteMapper;

    @Override
    public Paciente guardar(Paciente paciente) {
        PacienteEntity entity = springDataPacienteRepository.findById(paciente.getPersonaId())
                .orElseGet(() -> {
                    PersonaEntity personaEntity = springDataPersonaRepository.findById(paciente.getPersonaId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No existe PersonaEntity para personaId: " + paciente.getPersonaId()
                            ));

                    PacienteEntity nuevo = new PacienteEntity();
                    nuevo.setPersona(personaEntity);
                    return nuevo;
                });

        PacienteEntity saved = springDataPacienteRepository.save(entity);
        return pacienteMapper.toDomain(saved);
    }

    @Override
    public Optional<Paciente> buscarPorPersonaId(Long personaId) {
        return springDataPacienteRepository.findById(personaId)
                .map(pacienteMapper::toDomain);
    }

    @Override
    public boolean existsByPersonaId(Long personaId) {
        return springDataPacienteRepository.existsById(personaId);
    }

    @Override
    public List<Paciente> listarTodos() {
        return springDataPacienteRepository.findAll()
                .stream()
                .map(pacienteMapper::toDomain)
                .toList();
    }
}