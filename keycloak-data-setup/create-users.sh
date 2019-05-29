#!/bin/bash

source "$(dirname $0)/login.sh"

NUM_USERS="${1:-5}"

for count in $(seq 0 $((${NUM_USERS}-1))); do
  location_header=$(curl -sS -D - "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d "{\"username\":\"user-${count}\", \"enabled\":\"true\"}" | grep -Fi 'Location:')

  user_id=$(echo "${location_header##*/}" | tr -d '\r')

  curl -sS -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${user_id}/reset-password" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d '{"type":"password", "value":"user", "temporary":false}'
done
