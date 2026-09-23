package com.piedrazul.personas.application.service;

import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.domain.repository.IDisponibilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarDisponibilidadesService {

    private final IDisponibilidadRepository repository;

    public List<Disponibilidad> consultarTodas() {
        return repository.buscarTodas();
    }
}