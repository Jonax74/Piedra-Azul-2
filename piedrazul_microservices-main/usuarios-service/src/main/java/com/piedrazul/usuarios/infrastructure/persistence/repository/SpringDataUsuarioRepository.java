package com.piedrazul.usuarios.infrastructure.persistence.repository;

import com.piedrazul.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByUsername(String username);

    Optional<UsuarioEntity> findByKeycloakUserId(UUID keycloakUserId);

    Optional<UsuarioEntity> findByPersonaId(Long personaId);

    boolean existsByUsername(String username);

    boolean existsByPersonaId(Long personaId);
}
