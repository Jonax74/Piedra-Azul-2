package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.EstadoMedico;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.repository.IMedicoEspecialidadRepository;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEntity;
import com.piedrazul.personas.infrastructure.persistence.entity.PersonaEntity;
import com.piedrazul.personas.infrastructure.persistence.mapper.MedicoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicoRepositoryImpl implements IMedicoRepository {

    private final SpringDataMedicoRepository springDataMedicoRepository;
    private final SpringDataPersonaRepository springDataPersonaRepository;
    private final IMedicoEspecialidadRepository medicoEspecialidadRepository;
    private final MedicoMapper medicoMapper;

    @Override
    public Medico guardar(Medico medico) {
        MedicoEntity entity = springDataMedicoRepository.findById(medico.getPersonaId())
                .orElseGet(() -> {
                    PersonaEntity personaEntity = springDataPersonaRepository.findById(medico.getPersonaId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No existe PersonaEntity para personaId: " + medico.getPersonaId()
                            ));

                    MedicoEntity nuevo = new MedicoEntity();
                    nuevo.setPersona(personaEntity);
                    return nuevo;
                });

        entity.setTipoProfesional(medico.getTipoProfesional());
        entity.setEstado(medico.getEstado());

        MedicoEntity saved = springDataMedicoRepository.save(entity);
        return enriquecerConEspecialidades(medicoMapper.toDomain(saved));
    }

    @Override
    public Optional<Medico> buscarPorPersonaId(Long personaId) {
        return springDataMedicoRepository.findById(personaId)
                .map(medicoMapper::toDomain)
                .map(this::enriquecerConEspecialidades);
    }

    @Override
    public boolean existePorPersonaId(Long personaId) {
        return springDataMedicoRepository.existsByPersonaId(personaId);
    }

    @Override
    public List<Medico> listarTodos() {
        return springDataMedicoRepository.findAll()
                .stream()
                .map(medicoMapper::toDomain)
                .map(this::enriquecerConEspecialidades)
                .toList();
    }

    @Override
    public List<Medico> listarActivos() {
        return springDataMedicoRepository.findByEstado(EstadoMedico.ACTIVO)
                .stream()
                .map(medicoMapper::toDomain)
                .map(this::enriquecerConEspecialidades)
                .toList();
    }

    private Medico enriquecerConEspecialidades(Medico medico) {
        medico.setEspecialidades(medicoEspecialidadRepository.buscarPorMedicoId(medico.getPersonaId()));
        return medico;
    }
}