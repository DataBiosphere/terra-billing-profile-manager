package bio.terra.profile.pact.provider;

import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ProviderStateData {

  static BillingProfile gcpBillingProfile =
      new BillingProfile(
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
          Instant.now(),
          Instant.now(),
          "");

  static ProfileDescription gcpBillingProfileDescription =
      new ProfileDescription(gcpBillingProfile, Optional.of(new TpsPolicyInputs()));

  static BillingProfile azureBillingProfile =
      new BillingProfile(
          // Using hard-coded id, so as to avoid changing the pact with randomly generated data
          UUID.fromString("2f1d54d4-493d-4d5c-87ab-f06aca532abd"),
          "Azure Profile",
          "an Azure billing profile",
          "a biller?",
          CloudPlatform.AZURE,
          Optional.empty(),
          Optional.of(UUID.fromString("0f0ca5fb-03f1-4bd2-9021-c7ea009904dd")),
          Optional.of(UUID.fromString("58cf3ba4-538f-48d3-bf77-d1bdb0e504ff")),
          Optional.of(("52362072-0f91-469e-aaab-e7026ed4eb7c")),
          Instant.now(),
          Instant.now(),
          "");

  static ProfileDescription azureBillingProfileDescription =
      new ProfileDescription(azureBillingProfile, Optional.of(new TpsPolicyInputs()));
}
