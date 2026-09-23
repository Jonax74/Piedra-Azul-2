package com.piedrazul.personas.infrastructure.persistence.repository;

import com.piedrazul.personas.infrastructure.persistence.entity.PersonaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataPersonaRepository extends JpaRepository<PersonaEntity, Long> {

    Optional<PersonaEntity> findByDni(String dni);

    boolean existsByDni(String dni);
}