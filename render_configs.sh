#!/bin/bash

ENV=${1:-dev}
VAULT_TOKEN=${2:-$(cat "$HOME"/.vault-token)}

VAULT_ADDR="https://clotho.broadinstitute.org:8200"

VAULT_DOCKER_IMAGE="vault:1.11.2"

LOCAL_CONFIG_OUTPUT="service/src/main/resources/generated/local-properties.yaml"

VAULT_COMMAND="docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN -e VAULT_ADDR=$VAULT_ADDR $VAULT_DOCKER_IMAGE vault read"

function azure_creds {
  AZURE_VAULT_PATH="secret/dsde/terra/azure/dev/billingprofilemanager/managed-app-publisher"
  $VAULT_COMMAND -format=yaml $AZURE_VAULT_PATH \
  | yq '.out.env.azure.managedAppClientId = .data.client-id | .out.env.azure.managedAppClientSecret = .data.client-secret  | .out.env.azure.managedAppTenantId = .data.tenant-id | .out' \
  > $LOCAL_CONFIG_OUTPUT
}

azure_creds
