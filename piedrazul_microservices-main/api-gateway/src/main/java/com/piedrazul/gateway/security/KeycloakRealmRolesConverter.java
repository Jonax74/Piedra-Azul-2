package com.piedrazul.gateway.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte el JWT emitido por Keycloak en un Authentication que Spring Security
 * pueda usar para autorizar por rol.
 *
 * Keycloak coloca los realm roles en el claim:
 *   {
 *     "realm_access": {
 *       "roles": ["ADMINISTRADOR", "default-roles-piedrazul", "offline_access", "uma_authorization"]
 *     }
 *   }
 *
 * Este converter:
 *   1. Lee realm_access.roles del JWT.
 *   2. Mapea cada rol a una GrantedAuthority con prefijo "ROLE_" (convencion de Spring Security
 *      para que .hasRole("ADMINISTRADOR") funcione sin tener que escribir el prefijo).
 *   3. Define el principal name = claim "usuario_id" (UUID de dominio que pusimos en Keycloak).
 *      Asi, en cualquier downstream basta con authentication.getName() para tener el id.
 *      Si el claim no existe (token sin migrar), cae al "sub" estandar de OIDC.
 *
 * No se filtran los roles internos de Keycloak (default-roles-*, offline_access, uma_authorization):
 * la matriz de autorizacion en SecurityConfig solo referencia los roles del negocio, asi que el
 * resto es ruido inofensivo y evitamos mantener una lista de exclusiones.
 */
public class KeycloakRealmRolesConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PRINCIPAL_CLAIM = "usuario_id";
    private static final String FALLBACK_PRINCIPAL_CLAIM = "sub";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
        String principal = extractPrincipal(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get(ROLES_CLAIM);
        if (!(rolesObj instanceof List<?> rolesList)) {
            return Collections.emptyList();
        }

        return ((List<String>) rolesList).stream()
                .map(role -> ROLE_PREFIX + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }

    private String extractPrincipal(Jwt jwt) {
        String usuarioId = jwt.getClaimAsString(PRINCIPAL_CLAIM);
        if (usuarioId != null && !usuarioId.isBlank()) {
            return usuarioId;
        }
        return jwt.getClaimAsString(FALLBACK_PRINCIPAL_CLAIM);
    }
}
