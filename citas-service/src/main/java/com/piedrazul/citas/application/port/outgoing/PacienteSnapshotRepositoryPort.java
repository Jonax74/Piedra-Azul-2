package com.piedrazul.citas.application.port.outgoing;

import com.piedrazul.citas.domain.model.PacienteSnapshot;
import com.piedrazul.citas.domain.valueobjects.PacienteId;
import java.util.Optional;

public interface PacienteSnapshotRepositoryPort {
    Optional<PacienteSnapshot> findById(PacienteId id);
    PacienteSnapshot save(PacienteSnapshot snapshot);
}