#!/bin/bash

KEYCLOAK_URL=http://localhost:10080/auth
REALM=gatling
ADMIN_USER=admin
ADMIN_PASS=admin

ADMIN_TOKEN=$(curl -sS "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_USER}" \
  -d 'grant_type=password' \
  -d 'client_id=admin-cli' | jq -r '.access_token')
