package com.piedrazul.citas.interfaces.rest.controller;

import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.application.port.incoming.*;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.domain.valueobjects.PacienteId;
import com.piedrazul.citas.interfaces.rest.dto.request.*;
import com.piedrazul.citas.interfaces.rest.dto.response.CitaRestResponse;
import com.piedrazul.citas.interfaces.rest.mapper.CitaRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CrearCitaManualUseCase crearCitaManualUseCase;
    private final CrearCitaAutonomaUseCase crearCitaAutonomaUseCase;

    private final CancelarCitaUseCase cancelarCitaUseCase;
    private final ReagendarCitaUseCase reagendarCitaUseCase;
    private final MarcarAsistenciaUseCase marcarAsistenciaUseCase;
    private final ConsultarSlotsDisponiblesUseCase consultarSlotsDisponiblesUseCase;
    private final ListarCitasUseCase listarCitasPorMedicoUseCase;

    private final CitaRestMapper mapper;

    // Endpoint de agendamiento manual
    @PostMapping("/manual")
    public ResponseEntity<CitaRestResponse> crearCitaManual(
            @Valid @RequestBody CrearCitaRestRequest request
    ) {

        log.info("REST - Creación MANUAL de cita");

        CitaResponse response =
                crearCitaManualUseCase.crearCitaManual(
                        mapper.toApplicationRequest(request)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toRestResponse(response));
    }

    // Endpoint de agendamiento autónomo
    @PostMapping("/autonoma")
    public ResponseEntity<CitaRestResponse> crearCitaAutonoma(
            @Valid @RequestBody CrearCitaRestRequest request
    ) {

        log.info("REST - Creación AUTÓNOMA de cita");

        CitaResponse response =
                crearCitaAutonomaUseCase.crearCitaAutonoma(
                        mapper.toApplicationRequest(request)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toRestResponse(response));
    }

    @PutMapping("/{citaId}/cancelar")
    public ResponseEntity<CitaRestResponse> cancelarCita(
            @PathVariable String citaId,
            @RequestParam String motivo) {
        log.info("REST - Solicitud de cancelación de cita: {}", citaId);

        CancelarCitaRestRequest request = CancelarCitaRestRequest.builder()
                .citaId(citaId)
                .motivo(motivo)
                .build();

        CitaResponse response = cancelarCitaUseCase.cancelarCita(mapper.toApplicationRequest(request));
        CitaRestResponse restResponse = mapper.toRestResponse(response);

        return ResponseEntity.ok(restResponse);
    }

    @PutMapping("/reagendar")
    public ResponseEntity<CitaRestResponse> reagendarCita(@Valid @RequestBody ReagendarCitaRestRequest request) {
        log.info("REST - Solicitud de reagendamiento de cita: {} para nueva fecha: {}",
                request.getCitaId(), request.getNuevaFechaHora());

        CitaResponse response = reagendarCitaUseCase.reagendarCita(mapper.toApplicationRequest(request));
        CitaRestResponse restResponse = mapper.toRestResponse(response);

        return ResponseEntity.ok(restResponse);
    }

    @PutMapping("/asistencia")
    public ResponseEntity<CitaRestResponse> marcarAsistencia(@Valid @RequestBody MarcarAsistenciaRestRequest request) {
        log.info("REST - Solicitud de marcación de asistencia para cita: {}, asistió: {}",
                request.getCitaId(), request.getAsistio());

        CitaResponse response = marcarAsistenciaUseCase.marcarAsistencia(mapper.toApplicationRequest(request));
        CitaRestResponse restResponse = mapper.toRestResponse(response);

        return ResponseEntity.ok(restResponse);
    }

    @GetMapping("/medicos/{medicoId}/slots")
    public List<LocalDateTime> obtenerSlots(
            @PathVariable Long medicoId,
            @RequestParam(required = false) Long pacienteId) {
        PacienteId pacienteIdVo = pacienteId != null ? PacienteId.of(pacienteId) : null;
        return consultarSlotsDisponiblesUseCase.consultar(MedicoId.of(medicoId), pacienteIdVo);
    }

    @GetMapping("/historial")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {

        try {

            LocalDate fechaInicioParsed = parseFechaParam(fechaInicio);
            LocalDate fechaFinParsed = parseFechaParam(fechaFin);

            if (fechaInicioParsed == null && fechaFinParsed == null && fecha != null && !fecha.isEmpty()) {
                LocalDate dia = LocalDate.parse(fecha);
                fechaInicioParsed = dia;
                fechaFinParsed = dia;
            }

            List<CitaRestResponse> response =
                    listarCitasPorMedicoUseCase
                            .listar(medicoId, pacienteId, fechaInicioParsed, fechaFinParsed)
                            .stream()
                            .map(mapper::toRestResponse)
                            .toList();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    private LocalDate parseFechaParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}