package bio.terra.profile.service.profile.model;

import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.model.CreateProfileRequest;
import bio.terra.profile.model.ProfileModel;
import bio.terra.profile.service.policy.TpsConversionUtils;
import java.util.Objects;
import java.util.Optional;

public record ProfileDescription(
    BillingProfile billingProfile, Optional<TpsPolicyInputs> policies) {
  public ProfileDescription {
    Objects.requireNonNull(billingProfile);
    Objects.requireNonNull(policies);
  }

  public ProfileDescription(BillingProfile billingProfile) {
    this(billingProfile, Optional.empty());
  }

  public static ProfileDescription fromApiCreateProfileRequest(CreateProfileRequest request) {
    return new ProfileDescription(
        BillingProfile.fromApiCreateProfileRequest(request),
        Optional.ofNullable(TpsConversionUtils.tpsFromBpmApiPolicyInputs(request.getPolicies())));
  }

  public ProfileModel toApiProfileModel() {
    return billingProfile
        .toApiProfileModel()
        .policies(TpsConversionUtils.bpmFromTpsPolicyInputs(policies.orElse(null)));
  }
}
