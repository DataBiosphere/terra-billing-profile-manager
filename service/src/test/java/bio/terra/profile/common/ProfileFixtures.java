package bio.terra.profile.common;

import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.Optional;
import java.util.UUID;

public class ProfileFixtures {

  public static BillingProfile createAzureBillingProfile(
      UUID tenantId, UUID subscriptionId, String mrgId) {
    return createAzureBillingProfile(tenantId, subscriptionId, mrgId, Optional.empty());
  }

  public static BillingProfile createAzureBillingProfile(
      UUID tenantId, UUID subscriptionId, String mrgId, Optional<TpsPolicyInputs> policies) {
    var bpId = UUID.randomUUID();
    return new BillingProfile(
        bpId,
        "fake-bp-name-" + bpId,
        "fake-description",
        "direct",
        CloudPlatform.AZURE,
        Optional.empty(),
        Optional.of(tenantId),
        Optional.of(subscriptionId),
        Optional.of(mrgId),
        null,
        null,
        null,
        policies);
  }

  public static BillingProfile createGcpBillingProfile(String billingAccountId) {
    return createGcpBillingProfile(billingAccountId, Optional.empty());
  }

  public static BillingProfile createGcpBillingProfile(
      String billingAccountId, Optional<TpsPolicyInputs> policies) {
    var bpId = UUID.randomUUID();
    return new BillingProfile(
        bpId,
        "fake-bp-name-" + bpId,
        "fake-description",
        "direct",
        CloudPlatform.GCP,
        Optional.of(billingAccountId),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null,
        null,
        null,
        policies);
  }
}
