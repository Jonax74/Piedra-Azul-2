#!/bin/bash
set -e

KC_SERVER="${KC_SERVER:-http://keycloak:8080}"
KC_REALM="${KC_REALM:-piedrazul}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
PROFILE_FILE="${PROFILE_FILE:-/scripts/user-profile.json}"

echo "Configurando User Profile (usuario_id, persona_id) en realm ${KC_REALM}..."

/opt/keycloak/bin/kcadm.sh config credentials \
  --server "${KC_SERVER}" \
  --realm master \
  --user "${KC_ADMIN}" \
  --password "${KC_ADMIN_PASSWORD}"

/opt/keycloak/bin/kcadm.sh update "realms/${KC_REALM}/users/profile" -f "${PROFILE_FILE}"

echo "User Profile actualizado."
