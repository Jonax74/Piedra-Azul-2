package com.piedrazul.personas.interfaces.rest.controller;

import com.piedrazul.personas.application.service.*;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.interfaces.rest.dto.request.*;
import com.piedrazul.personas.interfaces.rest.dto.response.MedicoResponse;
import com.piedrazul.personas.interfaces.rest.mapper.MedicoRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final RegistrarMedicoService registrarMedicoService;
    private final ConsultarMedicoService consultarMedicoService;
    private final CambiarEstadoMedicoService cambiarEstadoMedicoService;
    private final AsignarEspecialidadesMedicoService asignarEspecialidadesMedicoService;
    private final MedicoRestMapper mapper;

    @PostMapping
    public MedicoResponse registrar(@Valid @RequestBody RegistrarMedicoRequest request) {
        // Cambiar de ejecutar() a registrarMedico()
        Medico medico = registrarMedicoService.registrarMedico(request);
        return mapper.toResponse(medico);
    }

    @GetMapping("/{personaId}")
    public MedicoResponse buscar(@PathVariable Long personaId) {
        return mapper.toResponse(consultarMedicoService.buscarPorPersonaId(personaId));
    }

    @GetMapping
    public List<MedicoResponse> listar() {
        return consultarMedicoService.listarTodos()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/activos")
    public List<MedicoResponse> listarActivos() {
        return consultarMedicoService.listarActivos()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PatchMapping("/{personaId}/estado")
    public MedicoResponse cambiarEstado(
            @PathVariable Long personaId,
            @Valid @RequestBody CambiarEstadoMedicoRequest request
    ) {
        Medico medico = cambiarEstadoMedicoService.ejecutar(
                personaId,
                request.getEstado()
        );
        return mapper.toResponse(medico);
    }

    @PutMapping("/{personaId}/especialidades")
    public MedicoResponse asignarEspecialidades(
            @PathVariable Long personaId,
            @Valid @RequestBody AsignarEspecialidadesMedicoRequest request
    ) {
        Medico medico = asignarEspecialidadesMedicoService.ejecutar(
                personaId,
                request.getEspecialidades()
        );
        return mapper.toResponse(medico);
    }
}