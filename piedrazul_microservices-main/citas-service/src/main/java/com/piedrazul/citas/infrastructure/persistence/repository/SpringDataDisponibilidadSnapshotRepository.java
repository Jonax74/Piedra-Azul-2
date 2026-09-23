package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.infrastructure.persistence.entity.DisponibilidadSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDisponibilidadSnapshotRepository extends JpaRepository<DisponibilidadSnapshotEntity, Long> {
}