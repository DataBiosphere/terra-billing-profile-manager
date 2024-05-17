#!/bin/bash
# Render a client SA for TPS connectivity and Azure managed app creds. for connectivity w/deployed MRGs

ENV=${1:-dev}

LOCAL_CONFIG_OUTPUT="service/src/main/resources/generated/local-properties.yaml"

function azure_creds {
  echo -n "Fetching azure credentials..."
  gcloud secrets versions access latest --project=broad-dsde-${ENV} --secret=bpm-managed-app-publisher-creds \
  | yq '.out.env.azure.managedAppClientId = .client-id | .out.env.azure.managedAppClientSecret = .client-secret  | .out.env.azure.managedAppTenantId = .tenant-id | .out' \
  > ${LOCAL_CONFIG_OUTPUT}
  echo "done."
}

function service_creds {
  # this SA key is required for connectivity to TPS for policy management
  echo -n "Fetching service credentials..."
  LOCAL_TPS_SA_PATH="service/src/main/resources/generated/bpm-client-sa.json"
  gcloud secrets versions access latest --project=broad-dsde-${ENV} --secret=bpm-sa-secret \
  | jq -r '."key.json"' > ${LOCAL_TPS_SA_PATH}
  echo "done."
}

azure_creds
service_creds

