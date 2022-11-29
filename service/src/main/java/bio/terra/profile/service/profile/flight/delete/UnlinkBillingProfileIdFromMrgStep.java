package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;

/** Removes the tag that associates a billing profile ID with an azure managed resource group */
public record UnlinkBillingProfileIdFromMrgStep(
    ApplicationService applicationService, BillingProfile profile) implements Step {
  private static final String BILLING_PROFILE_ID_TAG = "terra.billingProfileId";

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      var tenantId = profile.getRequiredTenantId();
      var subscriptionId = profile.getRequiredSubscriptionId();
      var mrgId = profile.getRequiredManagedResourceGroupId();

      applicationService.removeTagFromMrg(tenantId, subscriptionId, mrgId, BILLING_PROFILE_ID_TAG);

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
