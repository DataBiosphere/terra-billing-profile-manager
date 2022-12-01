package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;

/** Add a tag that associates a billing profile ID with an azure managed resource group */
public record LinkBillingProfileIdToMrgStep(
    ApplicationService applicationService,
    SamService samService,
    AuthenticatedUserRequest userRequest,
    BillingProfile profile)
    implements Step {
  private static final String BILLING_PROFILE_ID_TAG = "terra.billingProfileId";

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      var tenantId = profile.getRequiredTenantId();
      var subscriptionId = profile.getRequiredSubscriptionId();
      var mrgId = profile.getRequiredManagedResourceGroupId();

      // TODO remove after TOAZ-291
      applicationService.addTagToMrg(
          tenantId, subscriptionId, mrgId, BILLING_PROFILE_ID_TAG, profile.id().toString());

      samService.linkMrgToProfile(
          userRequest,
          profile.id(),
          profile.getRequiredManagedResourceGroupId(),
          profile.getRequiredSubscriptionId(),
          profile.getRequiredTenantId());

      return StepResult.getStepResultSuccess();
    } catch (Exception ex) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, ex);
    }
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
