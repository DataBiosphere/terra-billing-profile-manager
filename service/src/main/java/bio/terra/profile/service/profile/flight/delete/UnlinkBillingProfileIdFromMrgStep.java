package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.profile.flight.MRGTags;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;

/** Removes the tag that associates a billing profile ID with an azure managed resource group */
public record UnlinkBillingProfileIdFromMrgStep(
    ApplicationService applicationService, BillingProfile profile) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      var tenantId = profile.getRequiredTenantId();
      var subscriptionId = profile.getRequiredSubscriptionId();
      var mrgId = profile.getRequiredManagedResourceGroupId();

      applicationService.removeTagFromMrg(
          tenantId, subscriptionId, mrgId, MRGTags.BILLING_PROFILE_ID);

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
