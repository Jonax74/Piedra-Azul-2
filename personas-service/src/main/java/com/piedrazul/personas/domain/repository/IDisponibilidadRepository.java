package com.piedrazul.personas.domain.repository;

import com.piedrazul.personas.domain.model.Disponibilidad;
import java.util.List;
import java.util.Optional;

public interface IDisponibilidadRepository {

    Disponibilidad guardar(Disponibilidad disponibilidad);

    Optional<Disponibilidad> buscarPorId(Long id);

    List<Disponibilidad> buscarPorMedicoId(Long medicoId);

    void eliminar(Long id);

    boolean existePorMedicoIdYDia(Long medicoId, String diaSemana);

    boolean existePorMedicoIdYDiaExcluyendoId(Long medicoId, String diaSemana, Long id);

    List<Disponibilidad> buscarTodas();
}