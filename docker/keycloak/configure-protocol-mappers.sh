#!/bin/bash
set -e

KC_SERVER="${KC_SERVER:-http://keycloak:8080}"
KC_REALM="${KC_REALM:-piedrazul}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
CLIENT_ID="${KC_FRONTEND_CLIENT_ID:-piedrazul-frontend}"

echo "Verificando protocol mappers en cliente ${CLIENT_ID}..."

/opt/keycloak/bin/kcadm.sh config credentials \
  --server "${KC_SERVER}" \
  --realm master \
  --user "${KC_ADMIN}" \
  --password "${KC_ADMIN_PASSWORD}"

CLIENT_UUID="$(/opt/keycloak/bin/kcadm.sh get clients -r "${KC_REALM}" -q clientId="${CLIENT_ID}" --fields id --format csv --noquotes | tail -n 1)"

ensure_mapper() {
  local name="$1"
  local user_attr="$2"
  local claim_name="$3"
  local json_type="$4"

  if /opt/keycloak/bin/kcadm.sh get "clients/${CLIENT_UUID}/protocol-mappers/models" -r "${KC_REALM}" --fields name 2>/dev/null | grep -q "\"${name}\""; then
    echo "Mapper ${name} ya existe en ${CLIENT_ID}."
    return 0
  fi

  echo "Creando mapper ${name}..."
  /opt/keycloak/bin/kcadm.sh create "clients/${CLIENT_UUID}/protocol-mappers/models" -r "${KC_REALM}" \
    -s name="${name}" \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-usermodel-attribute-mapper \
    -s 'config."user.attribute"='"${user_attr}" \
    -s 'config."claim.name"='"${claim_name}" \
    -s 'config."jsonType.label"='"${json_type}" \
    -s 'config."id.token.claim"'=true \
    -s 'config."access.token.claim"'=true \
    -s 'config."userinfo.token.claim"'=true \
    -s 'config."introspection.token.claim"'=true
}

ensure_mapper "usuario_id-mapper" "usuario_id" "usuario_id" "String"
ensure_mapper "persona_id-mapper" "persona_id" "persona_id" "long"

echo "Protocol mappers listos."
