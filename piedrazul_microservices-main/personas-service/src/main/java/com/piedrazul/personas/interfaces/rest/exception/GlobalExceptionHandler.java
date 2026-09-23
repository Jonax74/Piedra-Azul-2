package com.piedrazul.personas.interfaces.rest.exception;

import com.piedrazul.personas.application.exception.*;
import com.piedrazul.personas.domain.model.EspecialidadMedica;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({PersonaNoEncontradaException.class, MedicoNoEncontradoException.class})
    public ResponseEntity<?> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({
            PersonaYaExisteException.class,
            PacienteYaRegistradoException.class,
            MedicoYaRegistradoException.class,
            ReglaDeNegocioException.class
    })
    public ResponseEntity<?> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DisponibilidadNoEncontradaException.class)
    public ResponseEntity<?> handleDisponibilidadNoEncontrada(DisponibilidadNoEncontradaException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(Map.of("error", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonInvalido(HttpMessageNotReadableException ex) {
        String detalle = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        String valoresValidos = Arrays.stream(EspecialidadMedica.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        String mensaje = "JSON inválido o especialidad no reconocida. "
                + "Use exactamente uno de estos valores: " + valoresValidos;

        if (detalle != null && detalle.contains("FISIOTERAPIA")) {
            mensaje += ". ¿Quiso decir FISIOTERAPEUTA?";
        }

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", mensaje,
                        "detalle", detalle != null ? detalle : ""
                ));
    }
}