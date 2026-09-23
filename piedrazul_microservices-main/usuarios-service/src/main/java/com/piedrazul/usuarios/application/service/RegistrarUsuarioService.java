package com.piedrazul.usuarios.application.service;

import com.piedrazul.usuarios.application.exception.PersonaNoEncontradaException;
import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.domain.repository.IUsuarioRepository;
import com.piedrazul.usuarios.infrastructure.keycloak.KeycloakAdminClient;
import com.piedrazul.usuarios.service.client.PersonaServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Caso de uso: registrar un nuevo usuario.
 *
 * Flujo (post-Keycloak):
 *   1. Validar inputs basicos (campos requeridos, formato).
 *   2. Validar reglas de unicidad locales (username, personaId).
 *   3. Verificar que la persona exista en personas-service.
 *   4. Generar UUID de dominio (usuario_id).
 *   5. Crear el usuario en Keycloak con atributos {usuario_id, persona_id}.
 *      → asignar realm roles
 *      → setear password permanente
 *   6. Persistir la fila local en 'usuario' enlazando el UUID local con el
 *      keycloakUserId devuelto por Keycloak.
 *   7. Si el paso 6 falla, COMPENSAR eliminando el usuario en Keycloak para
 *      no dejar usuarios huerfanos.
 *
 * Por que no usamos @Transactional para todo: porque las llamadas a Keycloak
 * y personas-service NO son transaccionales con la BD local. Manejamos la
 * inconsistencia explicitamente con la compensacion del paso 7.
 */
@Service
public class RegistrarUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(RegistrarUsuarioService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private final IUsuarioRepository usuarioRepository;
    private final PersonaServiceClient personaServiceClient;
    private final KeycloakAdminClient keycloakAdminClient;

    public RegistrarUsuarioService(
            IUsuarioRepository usuarioRepository,
            PersonaServiceClient personaServiceClient,
            KeycloakAdminClient keycloakAdminClient
    ) {
        this.usuarioRepository = usuarioRepository;
        this.personaServiceClient = personaServiceClient;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public Usuario ejecutar(
            Long personaId,
            String username,
            String password,
            String email,
            String firstName,
            String lastName,
            List<String> roles
    ) {
        validarEntrada(personaId, username, password, email, roles);

        String usernameNormalizado = username.trim();

        if (usuarioRepository.existePorUsername(usernameNormalizado)) {
            throw new IllegalArgumentException("El username ya existe");
        }
        if (usuarioRepository.existePorPersonaId(personaId)) {
            throw new IllegalArgumentException("Ya existe un usuario para ese personaId");
        }
        if (!personaServiceClient.existePersona(personaId.intValue(), true)) {
            throw new PersonaNoEncontradaException("Persona con id " + personaId + " no encontrada");
        }

        UUID usuarioId = UUID.randomUUID();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("usuario_id", usuarioId.toString());
        attributes.put("persona_id", String.valueOf(personaId));

        UUID keycloakUserId = keycloakAdminClient.crearUsuario(
                usernameNormalizado, email, firstName, lastName, attributes, password);

        try {
            keycloakAdminClient.setPasswordPermanente(keycloakUserId, password);
            keycloakAdminClient.limpiarAccionesRequeridas(keycloakUserId);
            keycloakAdminClient.asignarRealmRoles(keycloakUserId, roles);

            return persistirLocal(usuarioId, keycloakUserId, usernameNormalizado, personaId);

        } catch (RuntimeException ex) {
            log.error("Fallo despues de crear el usuario en Keycloak ({}). Compensando...",
                    keycloakUserId, ex);
            compensarKeycloak(keycloakUserId);
            throw ex;
        }
    }

    @Transactional
    protected Usuario persistirLocal(
            UUID usuarioId, UUID keycloakUserId, String username, Long personaId
    ) {
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .keycloakUserId(keycloakUserId)
                .username(username)
                .personaId(personaId)
                .build();
        return usuarioRepository.guardar(usuario);
    }

    private void compensarKeycloak(UUID keycloakUserId) {
        try {
            keycloakAdminClient.eliminarUsuario(keycloakUserId);
        } catch (RuntimeException compensationError) {
            log.error("FALLO COMPENSACION: el usuario {} quedo huerfano en Keycloak. "
                    + "Requiere limpieza manual.", keycloakUserId, compensationError);
        }
    }

    private void validarEntrada(
            Long personaId, String username, String password, String email, List<String> roles
    ) {
        if (personaId == null) {
            throw new IllegalArgumentException("personaId es obligatorio");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username es obligatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password es obligatorio");
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("email invalido");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debe asignarse al menos un rol");
        }
        for (String r : roles) {
            if (r == null || r.isBlank()) {
                throw new IllegalArgumentException("Nombre de rol invalido");
            }
        }
    }
}
