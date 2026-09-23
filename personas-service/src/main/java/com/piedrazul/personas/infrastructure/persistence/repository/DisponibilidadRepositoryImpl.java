package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.domain.repository.IDisponibilidadRepository;
import com.piedrazul.personas.infrastructure.persistence.entity.DisponibilidadEntity;
import com.piedrazul.personas.infrastructure.persistence.mapper.DisponibilidadMapper;
import com.piedrazul.personas.infrastructure.persistence.springdata.SpringDataDisponibilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DisponibilidadRepositoryImpl implements IDisponibilidadRepository {

    private final SpringDataDisponibilidadRepository springDataRepository;
    private final DisponibilidadMapper mapper;

    @Override
    public Disponibilidad guardar(Disponibilidad disponibilidad) {
        DisponibilidadEntity entity = mapper.toEntity(disponibilidad);
        DisponibilidadEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Disponibilidad> buscarPorId(Long id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Disponibilidad> buscarPorMedicoId(Long medicoId) {
        return springDataRepository.findByMedicoId(medicoId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existePorMedicoIdYDia(Long medicoId, String diaSemana) {
        return springDataRepository.existsByMedicoIdAndDiaSemana(medicoId, diaSemana);
    }

    @Override
    public boolean existePorMedicoIdYDiaExcluyendoId(Long medicoId, String diaSemana, Long id) {
        return springDataRepository.existsByMedicoIdAndDiaSemanaAndIdNot(medicoId, diaSemana, id);
    }

    @Override
    public List<Disponibilidad> buscarTodas() {

        return springDataRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}