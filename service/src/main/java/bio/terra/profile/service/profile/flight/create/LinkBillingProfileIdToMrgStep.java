package bio.terra.profile.service.profile.flight.create;

import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.profile.flight.MRGTags;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;

/** Add a tag that associates a billing profile ID with an azure managed resource group */
public record LinkBillingProfileIdToMrgStep(
    ApplicationService applicationService, BillingProfile profile) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      var tenantId = profile.getRequiredTenantId();
      var subscriptionId = profile.getRequiredSubscriptionId();
      var mrgId = profile.getRequiredManagedResourceGroupId();

      applicationService.addTagToMrg(
          tenantId, subscriptionId, mrgId, MRGTags.BILLING_PROFILE_ID, profile.id().toString());

      return StepResult.getStepResultSuccess();
    } catch (Exception ex) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, ex);
    }
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
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
}
