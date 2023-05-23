package bio.terra.profile.pact.provider;

import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProviderStateData {



  static BillingProfile gcpBillingProfile = new BillingProfile(
      // Using hard-coded id, so as to avoid changing the pact with randomly generated data
      UUID.fromString("479e6559-027c-43b3-b422-09c18dcca3c8"),
      "GCP Profile",
      "a GCP billing profile",
      "a biller?",
      CloudPlatform.GCP,
      Optional.of("some-billing-account"),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Instant.now(),  // todo: don't make these random
      Instant.now(), // todo: don't make these random
      ""
  );

  static BillingProfile azureBillingProfile = new BillingProfile(
      // Using hard-coded id, so as to avoid changing the pact with randomly generated data
      UUID.fromString("479e6559-027c-43b3-b422-09c18dcca3c8"),
      "GCP Profile",
      "a GCP billing profile",
      "a biller?",
      CloudPlatform.GCP,
      Optional.empty(),
      Optional.of(UUID.fromString("0f0ca5fb-03f1-4bd2-9021-c7ea009904dd")),
      Optional.of(UUID.fromString("58cf3ba4-538f-48d3-bf77-d1bdb0e504ff")),
      Optional.of(("52362072-0f91-469e-aaab-e7026ed4eb7c")),
      Instant.now(),  // todo: don't make these random
      Instant.now(), // todo: don't make these random
      ""
  );

  //
  /*
         "managedResourceGroupId": "string",
          "tenantId": "e2490de5-5bd3-43d5-b7c4-526e33f71304",
          "id": "e2490de5-5bd3-43d5-b7c4-526e33f71304",
          "subscriptionId": "e2490de5-5bd3-43d5-b7c4-526e33f71304"
   */
  static Map<String, Object> providerStateValues = Map.of(
      "gcpProfileId", gcpBillingProfile.id().toString(),
      "azureProfileId", azureBillingProfile.id().toString(),
      "tenantId", azureBillingProfile.tenantId().get().toString(),
      "subscriptionId", azureBillingProfile.subscriptionId().get().toString(),
      "managedResourceGroupId", azureBillingProfile.managedResourceGroupId().get()
  );

}
