package com.piedrazul.personas.infrastructure.persistence.springdata;

import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEspecialidadEntity;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEspecialidadId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMedicoEspecialidadRepository extends JpaRepository<MedicoEspecialidadEntity, MedicoEspecialidadId> {

    List<MedicoEspecialidadEntity> findByIdMedicoId(Long medicoId);

    void deleteByIdMedicoId(Long medicoId);
}
