#!/bin/bash
set -e

SCRIPT_DIR="${SCRIPT_DIR:-/scripts}"

run_script() {
  local file="$1"
  echo "==> Ejecutando $(basename "${file}")"
  tr -d '\r' < "${file}" | bash
}

run_script "${SCRIPT_DIR}/configure-service-account.sh"
run_script "${SCRIPT_DIR}/configure-user-profile.sh"
run_script "${SCRIPT_DIR}/configure-protocol-mappers.sh"

echo "Configuracion base de Keycloak completada."
