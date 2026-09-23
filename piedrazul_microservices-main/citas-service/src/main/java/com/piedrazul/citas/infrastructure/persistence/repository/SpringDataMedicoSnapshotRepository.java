package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.infrastructure.persistence.entity.MedicoSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataMedicoSnapshotRepository extends JpaRepository<MedicoSnapshotEntity, Long> {
}