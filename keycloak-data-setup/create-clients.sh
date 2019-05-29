#!/bin/bash

source "$(dirname $0)/login.sh"

NUM_CLIENTS="${1:-3}"

for count in $(seq 0 $((${NUM_CLIENTS}-1))); do
  curl "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d "{\"clientId\":\"client-app-${count}\", \"publicClient\":\"true\", \"redirectUris\":[\"http://client-app-${count}/*\"]}"
done