package com.piedrazul.citas.interfaces.rest.controller;

import com.piedrazul.citas.application.service.ValidarCambioDisponibilidadService;
import com.piedrazul.citas.interfaces.rest.dto.request.ValidarEliminacionDisponibilidadRequest;
import com.piedrazul.citas.interfaces.rest.dto.request.ValidarModificacionDisponibilidadRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citas/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadValidacionController {

    private final ValidarCambioDisponibilidadService validarCambioDisponibilidadService;

    @PostMapping("/validar-eliminacion")
    public ResponseEntity<Void> validarEliminacion(
            @Valid @RequestBody ValidarEliminacionDisponibilidadRequest request
    ) {
        validarCambioDisponibilidadService.validarEliminacion(
                request.getMedicoId(),
                request.getDiaSemana(),
                request.getHoraInicio(),
                request.getHoraFin()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validar-modificacion")
    public ResponseEntity<Void> validarModificacion(
            @Valid @RequestBody ValidarModificacionDisponibilidadRequest request
    ) {
        validarCambioDisponibilidadService.validarModificacion(
                request.getMedicoIdActual(),
                request.getDiaSemanaActual(),
                request.getHoraInicioActual(),
                request.getHoraFinActual(),
                request.getMedicoIdNuevo(),
                request.getDiaSemanaNuevo(),
                request.getHoraInicioNuevo(),
                request.getHoraFinNuevo()
        );
        return ResponseEntity.ok().build();
    }
}
