package com.piedrazul.citas.application.port.outgoing;

import com.piedrazul.citas.domain.model.MedicoSnapshot;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import java.util.Optional;

public interface MedicoSnapshotRepositoryPort {
    Optional<MedicoSnapshot> findById(MedicoId id);
    MedicoSnapshot save(MedicoSnapshot snapshot);
}