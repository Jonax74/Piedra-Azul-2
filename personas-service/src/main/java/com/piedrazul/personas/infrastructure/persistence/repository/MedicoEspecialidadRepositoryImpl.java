package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.EspecialidadMedica;
import com.piedrazul.personas.domain.repository.IMedicoEspecialidadRepository;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEspecialidadEntity;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEspecialidadId;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEntity;
import com.piedrazul.personas.infrastructure.persistence.springdata.SpringDataMedicoEspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MedicoEspecialidadRepositoryImpl implements IMedicoEspecialidadRepository {

    private final SpringDataMedicoEspecialidadRepository springDataRepository;
    private final SpringDataMedicoRepository springDataMedicoRepository;

    @Override
    @Transactional
    public void reemplazarEspecialidades(Long medicoId, Set<EspecialidadMedica> especialidades) {
        springDataRepository.deleteByIdMedicoId(medicoId);

        MedicoEntity medico = springDataMedicoRepository.findById(medicoId)
                .orElseThrow(() -> new IllegalStateException("No existe MedicoEntity para personaId: " + medicoId));

        for (EspecialidadMedica especialidad : especialidades) {
            MedicoEspecialidadEntity entity = new MedicoEspecialidadEntity();
            entity.setId(new MedicoEspecialidadId(medicoId, especialidad));
            entity.setMedico(medico);
            springDataRepository.save(entity);
        }
    }

    @Override
    public Set<EspecialidadMedica> buscarPorMedicoId(Long medicoId) {
        return springDataRepository.findByIdMedicoId(medicoId).stream()
                .map(entity -> entity.getId().getEspecialidad())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
