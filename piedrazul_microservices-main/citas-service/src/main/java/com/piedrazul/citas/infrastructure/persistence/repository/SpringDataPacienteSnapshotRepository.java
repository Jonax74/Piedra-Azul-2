package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.infrastructure.persistence.entity.PacienteSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPacienteSnapshotRepository extends JpaRepository<PacienteSnapshotEntity, Long> {
}