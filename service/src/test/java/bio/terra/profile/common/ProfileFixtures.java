package bio.terra.profile.common;

import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
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

  public static ProfileDescription createAzureBillingProfileDescription(
      UUID tenantId, UUID subscriptionId, String mrgId, TpsPolicyInputs policies) {
    return new ProfileDescription(
        createAzureBillingProfile(tenantId, subscriptionId, mrgId), Optional.ofNullable(policies));
  }

  public static ProfileDescription createAzureBillingProfileDescription(
      UUID tenantId, UUID subscriptionId, String mrgId) {
    return new ProfileDescription(
        createAzureBillingProfile(tenantId, subscriptionId, mrgId), Optional.empty());
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

  public static ProfileDescription createGcpBillingProfileDescription(
      String billingAccountId, TpsPolicyInputs policies) {
    return new ProfileDescription(
        createGcpBillingProfile(billingAccountId), Optional.ofNullable(policies));
  }

  public static ProfileDescription createGcpBillingProfileDescription(String billingAccountId) {
    return new ProfileDescription(createGcpBillingProfile(billingAccountId), Optional.empty());
  }
}
