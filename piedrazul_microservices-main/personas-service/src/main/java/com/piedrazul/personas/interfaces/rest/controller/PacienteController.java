package com.piedrazul.personas.interfaces.rest.controller;

import com.piedrazul.personas.application.service.RegistrarPacienteService;
import com.piedrazul.personas.application.service.ConsultarPacienteService;
import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.interfaces.rest.dto.request.RegistrarPacienteRequest;
import com.piedrazul.personas.interfaces.rest.dto.response.PacienteResponse;
import com.piedrazul.personas.interfaces.rest.mapper.PacienteRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final RegistrarPacienteService registrarPacienteService;
    private final ConsultarPacienteService consultarPacienteService;
    private final PacienteRestMapper mapper;

    @PostMapping
    public PacienteResponse registrar(@Valid @RequestBody RegistrarPacienteRequest request) {
        Paciente paciente = registrarPacienteService.registrarPaciente(request);
        return mapper.toResponse(paciente);
    }

    @GetMapping("/{personaId}")
    public PacienteResponse buscar(@PathVariable Long personaId) {
        return mapper.toResponse(consultarPacienteService.buscarPorPersonaId(personaId));
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return consultarPacienteService.listarTodos()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}