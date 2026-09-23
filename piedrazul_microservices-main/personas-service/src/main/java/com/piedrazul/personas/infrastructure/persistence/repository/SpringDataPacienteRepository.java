package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.infrastructure.persistence.entity.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPacienteRepository extends JpaRepository<PacienteEntity, Long> {

    boolean existsByPersonaId(Long personaId);
}