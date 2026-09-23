#!/bin/sh
set -eu

GATEWAY_URL="${GATEWAY_URL:-http://api-gateway:8085}"
KC_SERVER="${KC_SERVER:-http://keycloak:8080}"
KC_REALM="${KC_REALM:-piedrazul}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

json_field() {
  printf '%s' "$1" | tr -d '\n' | sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^,\"}]*\)\"\{0,1\}.*/\1/p" | head -n 1
}

json_number_field() {
  printf '%s' "$1" | tr -d '\n' | sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p" | head -n 1
}

get_admin_token() {
  local resp
  resp="$(curl -fsS -X POST "${KC_SERVER}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=admin-cli&username=${KC_ADMIN}&password=${KC_ADMIN_PASSWORD}")"
  json_field "${resp}" "access_token"
}

crear_persona() {
  local dni="$1"
  local nombre="$2"
  local apellido="$3"
  local resp
  resp="$(curl -fsS -X POST "${GATEWAY_URL}/api/personas" \
    -H "Content-Type: application/json" \
    -d "{\"primerNombre\":\"${nombre}\",\"primerApellido\":\"${apellido}\",\"genero\":\"HOMBRE\",\"fechaNacimiento\":\"1990-05-15\",\"telefono\":\"3001112233\",\"dni\":\"${dni}\"}")"
  json_number_field "${resp}" "id"
}

vincular_usuario() {
  local token="$1"
  local username="$2"
  local persona_id="$3"
  local usuario_id="$4"
  local first_name="$5"
  local last_name="$6"

  local users_resp user_uuid
  users_resp="$(curl -fsS -G "${KC_SERVER}/admin/realms/${KC_REALM}/users" \
    -H "Authorization: Bearer ${token}" \
    --data-urlencode "username=${username}")"

  user_uuid="$(printf '%s' "${users_resp}" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"

  if [ -z "${user_uuid}" ]; then
    echo "Usuario ${username} no encontrado, se omite."
    return 0
  fi

  # PUT parcial borra email/nombre en KC26; enviar perfil completo + atributos.
  curl -fsS -X PUT "${KC_SERVER}/admin/realms/${KC_REALM}/users/${user_uuid}" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${username}\",\"email\":\"${username}@piedrazul.local\",\"firstName\":\"${first_name}\",\"lastName\":\"${last_name}\",\"emailVerified\":true,\"enabled\":true,\"requiredActions\":[],\"attributes\":{\"usuario_id\":[\"${usuario_id}\"],\"persona_id\":[\"${persona_id}\"]}}" >/dev/null

  echo "Vinculado ${username} -> persona_id=${persona_id}, usuario_id=${usuario_id}"
}

echo "Sembrando personas de prueba y vinculando usuarios Keycloak..."

TOKEN="$(get_admin_token)"

PACIENTE_PERSONA_ID="$(crear_persona "9000000001" "Paciente" "Test")"
if [ -n "${PACIENTE_PERSONA_ID}" ]; then
  curl -fsS -X POST "${GATEWAY_URL}/api/pacientes" \
    -H "Content-Type: application/json" \
    -d "{\"personaId\":${PACIENTE_PERSONA_ID}}" >/dev/null
  vincular_usuario "${TOKEN}" "paciente.test" "${PACIENTE_PERSONA_ID}" "11111111-1111-1111-1111-111111111101" "Paciente" "Test"
fi

MEDICO_PERSONA_ID="$(crear_persona "9000000002" "Medico" "Test")"
if [ -n "${MEDICO_PERSONA_ID}" ]; then
  curl -fsS -X POST "${GATEWAY_URL}/api/medicos" \
    -H "Content-Type: application/json" \
    -d "{\"personaId\":${MEDICO_PERSONA_ID},\"tipoProfesional\":\"TERAPISTA\",\"especialidades\":[\"FISIOTERAPEUTA\"]}" >/dev/null
  vincular_usuario "${TOKEN}" "medico.test" "${MEDICO_PERSONA_ID}" "22222222-2222-2222-2222-222222222202" "Medico" "Test"
fi

AGENDADOR_PERSONA_ID="$(crear_persona "9000000003" "Agendador" "Test")"
if [ -n "${AGENDADOR_PERSONA_ID}" ]; then
  vincular_usuario "${TOKEN}" "agendador.test" "${AGENDADOR_PERSONA_ID}" "33333333-3333-3333-3333-333333333303" "Agendador" "Test"
fi

vincular_usuario "${TOKEN}" "admin.test" "0" "44444444-4444-4444-4444-444444444404" "Admin" "Test"

echo "Seed de usuarios de prueba completado."
