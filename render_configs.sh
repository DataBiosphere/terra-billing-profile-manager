#!/bin/bash

ENV=${1:-dev}
VAULT_TOKEN=${2:-$(cat "$HOME"/.vault-token)}

VAULT_ADDR="https://clotho.broadinstitute.org:8200"

LOCAL_CONFIG_OUTPUT="service/src/main/resources/generated/local-properties.yaml"

VAULT_COMMAND="docker run --rm -i -e VAULT_TOKEN="${VAULT_TOKEN}" broadinstitute/dsde-toolbox:dev vault read"

function azure_creds {
  AZURE_VAULT_PATH="secret/dsde/terra/azure/dev/billingprofilemanager/managed-app-publisher"
  $VAULT_COMMAND -format=yaml $AZURE_VAULT_PATH \
  | yq '.out.env.azure.managedAppClientId = .data.client-id | .out.env.azure.managedAppClientSecret = .data.client-secret  | .out.env.azure.managedAppTenantId = .data.tenant-id | .out' \
  > $LOCAL_CONFIG_OUTPUT
}


function service_creds {
  LOCAL_TPS_SA_PATH="service/src/main/resources/generated/bpm-client-sa.json"
  TPS_VAULT_PATH="secret/dsde/terra/kernel/dev/dev/bpm/app-sa"
  tempfile=$(mktemp)
  $VAULT_COMMAND -format=json "$TPS_VAULT_PATH" \
  | jq -r .data.key | base64 -d \
  > "$LOCAL_TPS_SA_PATH"
}

azure_creds
service_creds
