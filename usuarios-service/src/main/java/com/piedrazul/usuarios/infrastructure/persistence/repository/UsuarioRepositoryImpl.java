package com.piedrazul.usuarios.infrastructure.persistence.repository;

import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.domain.repository.IUsuarioRepository;
import com.piedrazul.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import com.piedrazul.usuarios.infrastructure.persistence.mapper.UsuarioMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class UsuarioRepositoryImpl implements IUsuarioRepository {

    private final SpringDataUsuarioRepository usuarioRepository;

    public UsuarioRepositoryImpl(SpringDataUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = UsuarioMapper.toEntity(usuario);
        UsuarioEntity persisted = usuarioRepository.save(entity);
        return UsuarioMapper.toDomain(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorKeycloakUserId(UUID keycloakUserId) {
        return usuarioRepository.findByKeycloakUserId(keycloakUserId).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorPersonaId(Long personaId) {
        return usuarioRepository.findByPersonaId(personaId).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorPersonaId(Long personaId) {
        return usuarioRepository.existsByPersonaId(personaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarPorId(UUID id) {
        usuarioRepository.deleteById(id);
    }
}
