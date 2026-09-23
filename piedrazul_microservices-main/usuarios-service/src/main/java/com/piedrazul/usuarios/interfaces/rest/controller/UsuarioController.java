package com.piedrazul.usuarios.interfaces.rest.controller;

import com.piedrazul.usuarios.application.service.ConsultarUsuarioService;
import com.piedrazul.usuarios.application.service.RegistrarUsuarioService;
import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.interfaces.rest.dto.request.CrearUsuarioRequest;
import com.piedrazul.usuarios.interfaces.rest.dto.response.UsuarioResponse;
import com.piedrazul.usuarios.interfaces.rest.mapper.UsuarioRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final RegistrarUsuarioService registrarUsuarioService;
    private final ConsultarUsuarioService consultarUsuarioService;

    public UsuarioController(
            RegistrarUsuarioService registrarUsuarioService,
            ConsultarUsuarioService consultarUsuarioService
    ) {
        this.registrarUsuarioService = registrarUsuarioService;
        this.consultarUsuarioService = consultarUsuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request
    ) {
        Usuario usuario = registrarUsuarioService.ejecutar(
                request.getPersonaId(),
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoles()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UsuarioRestMapper.toResponse(usuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable UUID id) {
        Usuario usuario = consultarUsuarioService.consultarPorId(id);
        return ResponseEntity.ok(UsuarioRestMapper.toResponse(usuario));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorUsername(@PathVariable String username) {
        Usuario usuario = consultarUsuarioService.consultarPorUsername(username);
        return ResponseEntity.ok(UsuarioRestMapper.toResponse(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        List<UsuarioResponse> response = consultarUsuarioService.listarTodos().stream()
                .map(UsuarioRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
