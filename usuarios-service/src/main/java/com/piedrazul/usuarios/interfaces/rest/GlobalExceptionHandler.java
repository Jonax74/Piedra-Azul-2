package com.piedrazul.usuarios.interfaces.rest;

import com.piedrazul.usuarios.application.exception.DependenciaExternaException;
import com.piedrazul.usuarios.application.exception.PersonaNoEncontradaException;
import com.piedrazul.usuarios.infrastructure.keycloak.KeycloakAdminClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errorBody(400, "Bad Request",
                "Error de validacion", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(errorBody(400, "Bad Request", ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, "Conflict", ex.getMessage(), null));
    }

    @ExceptionHandler(PersonaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handlePersonaNotFound(PersonaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, "Not Found", ex.getMessage(), null));
    }

    @ExceptionHandler(DependenciaExternaException.class)
    public ResponseEntity<Map<String, Object>> handleDependency(DependenciaExternaException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(503, "Service Unavailable", ex.getMessage(), null));
    }

    @ExceptionHandler(KeycloakAdminClient.UsuarioYaExisteException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioYaExiste(
            KeycloakAdminClient.UsuarioYaExisteException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, "Conflict", ex.getMessage(), null));
    }

    @ExceptionHandler(KeycloakAdminClient.KeycloakAdminException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakAdmin(
            KeycloakAdminClient.KeycloakAdminException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody(502, "Bad Gateway", "Error comunicandose con Keycloak: " + ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "Internal Server Error", ex.getMessage(), null));
    }

    private Map<String, Object> errorBody(int status, String error, String message, Map<String, String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }
}
