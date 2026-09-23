package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.domain.model.EstadoMedico;
import com.piedrazul.personas.infrastructure.persistence.entity.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMedicoRepository extends JpaRepository<MedicoEntity, Long> {

    boolean existsByPersonaId(Long personaId);

    List<MedicoEntity> findByEstado(EstadoMedico estado);
}