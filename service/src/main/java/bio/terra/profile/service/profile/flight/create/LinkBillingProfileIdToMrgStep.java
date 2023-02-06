package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

/** Add a tag that associates a billing profile ID with an azure managed resource group */
public record LinkBillingProfileIdToMrgStep(
    ApplicationService applicationService,
    SamService samService,
    BillingProfile profile,
    AuthenticatedUserRequest userRequest)
    implements Step {
  private static final String BILLING_PROFILE_ID_TAG = "terra.billingProfileId";

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    samService.createManagedResourceGroup(profile, userRequest);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    samService.deleteManagedResourceGroup(profile.id(), userRequest);

    return StepResult.getStepResultSuccess();
  }
}
