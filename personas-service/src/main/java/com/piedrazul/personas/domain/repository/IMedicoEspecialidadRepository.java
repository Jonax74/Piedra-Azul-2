package com.piedrazul.personas.domain.repository;

import com.piedrazul.personas.domain.model.EspecialidadMedica;

import java.util.Set;

public interface IMedicoEspecialidadRepository {

    void reemplazarEspecialidades(Long medicoId, Set<EspecialidadMedica> especialidades);

    Set<EspecialidadMedica> buscarPorMedicoId(Long medicoId);
}
