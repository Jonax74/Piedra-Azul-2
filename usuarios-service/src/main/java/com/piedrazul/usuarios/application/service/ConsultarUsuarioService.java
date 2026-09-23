package com.piedrazul.usuarios.application.service;

import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.domain.repository.IUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultarUsuarioService {

    private final IUsuarioRepository usuarioRepository;

    public ConsultarUsuarioService(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario consultarPorId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id es obligatorio");
        }
        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public Usuario consultarPorUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username es obligatorio");
        }
        return usuarioRepository.buscarPorUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.listarTodos();
    }
}
