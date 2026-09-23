#!/bin/bash
set -e

KC_SERVER="${KC_SERVER:-http://keycloak:8080}"
KC_REALM="${KC_REALM:-piedrazul}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
CLIENT_ID="${KC_ADMIN_CLIENT_ID:-piedrazul-admin-cli}"

echo "Configurando service account de ${CLIENT_ID} en realm ${KC_REALM}..."

/opt/keycloak/bin/kcadm.sh config credentials \
  --server "${KC_SERVER}" \
  --realm master \
  --user "${KC_ADMIN}" \
  --password "${KC_ADMIN_PASSWORD}"

CLIENT_UUID="$(/opt/keycloak/bin/kcadm.sh get clients -r "${KC_REALM}" -q clientId="${CLIENT_ID}" --fields id --format csv --noquotes | tail -n 1)"
SERVICE_USER_ID="$(/opt/keycloak/bin/kcadm.sh get "clients/${CLIENT_UUID}/service-account-user" -r "${KC_REALM}" --fields id --format csv --noquotes | tail -n 1)"
RM_CLIENT_ID="$(/opt/keycloak/bin/kcadm.sh get clients -r "${KC_REALM}" -q clientId=realm-management --fields id --format csv --noquotes | tail -n 1)"

for ROLE in manage-users view-users query-users view-realm; do
  /opt/keycloak/bin/kcadm.sh add-roles -r "${KC_REALM}" \
    --uid "${SERVICE_USER_ID}" \
    --cid "${RM_CLIENT_ID}" \
    --rolename "${ROLE}"
done

echo "Service account configurado correctamente."
