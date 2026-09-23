package com.piedrazul.usuarios.domain.repository;

import com.piedrazul.usuarios.domain.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUsuarioRepository {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorKeycloakUserId(UUID keycloakUserId);

    Optional<Usuario> buscarPorUsername(String username);

    Optional<Usuario> buscarPorPersonaId(Long personaId);

    boolean existePorUsername(String username);

    boolean existePorPersonaId(Long personaId);

    List<Usuario> listarTodos();

    void eliminarPorId(UUID id);
}
