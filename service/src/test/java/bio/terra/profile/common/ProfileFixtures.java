package bio.terra.profile.common;

import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.Optional;
import java.util.UUID;

public class ProfileFixtures {

  public static BillingProfile createAzureBillingProfile(
      UUID tenantId, UUID subscriptionId, String mrgId) {
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
        null);
  }

  public static BillingProfile createGcpBillingProfile(String billingAccountId) {
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
        null);
  }
}
