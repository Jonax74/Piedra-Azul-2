package com.piedrazul.citas.interfaces.rest.exception;

import com.piedrazul.citas.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PacienteNoExisteException.class)
    public ResponseEntity<ErrorResponse> handlePacienteNoExiste(PacienteNoExisteException ex) {
        log.error("Paciente no existe: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Paciente No Encontrado")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PacienteNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handlePacienteNoDisponible(PacienteNoDisponibleException ex) {
        log.warn("Paciente no disponible: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Paciente No Disponible")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MedicoNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleMedicoNoDisponible(MedicoNoDisponibleException ex) {
        log.error("Médico no disponible: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Médico No Disponible")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DisponibilidadNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleDisponibilidadNoDisponible(DisponibilidadNoDisponibleException ex) {
        log.error("Disponibilidad no disponible: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Horario No Disponible")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DisponibilidadConCitasActivasException.class)
    public ResponseEntity<ErrorResponse> handleDisponibilidadConCitasActivas(
            DisponibilidadConCitasActivasException ex,
            HttpServletRequest request
    ) {
        log.warn("Disponibilidad con citas activas: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Disponibilidad con Citas Activas")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CitaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleCitaNoEncontrada(CitaNoEncontradaException ex) {
        log.error("Cita no encontrada: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Cita No Encontrada")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CitaNoCancelableException.class)
    public ResponseEntity<ErrorResponse> handleCitaNoCancelable(CitaNoCancelableException ex) {
        log.error("Cita no cancelable: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Cita No Cancelable")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CitaNoReagendableException.class)
    public ResponseEntity<ErrorResponse> handleCitaNoReagendable(CitaNoReagendableException ex) {
        log.error("Cita no reagendable: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Cita No Reagendable")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CitaNoMarcableException.class)
    public ResponseEntity<ErrorResponse> handleCitaNoMarcable(CitaNoMarcableException ex) {
        log.error("Cita no marcable: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Cita No Marcable")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RestriccionAutoservicioException.class)
    public ResponseEntity<ErrorResponse> handleRestriccionAutoservicio(RestriccionAutoservicioException ex) {
        log.error("Restricción autoservicio: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Restricción de Autoservicio")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConsultaGeneralRequeridaException.class)
    public ResponseEntity<ErrorResponse> handleConsultaGeneralRequerida(ConsultaGeneralRequeridaException ex) {
        log.warn("Consulta general requerida: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Consulta General Requerida")
                .message(ex.getMessage())
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de Validación")
                .message("Datos de entrada inválidos")
                .validationErrors(errors)
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Error no manejado: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error Interno del Servidor")
                .message("Ha ocurrido un error inesperado")
                .path(getPath())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getPath() {
        // Esto se puede mejorar obteniendo del request actual
        return "Unknown path";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Solicitud inválida")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .validationErrors(null)
                .build();

        return ResponseEntity.badRequest().body(error);
    }
}