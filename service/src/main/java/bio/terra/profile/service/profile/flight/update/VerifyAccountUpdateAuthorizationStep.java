package bio.terra.profile.service.profile.flight.update;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public record VerifyAccountUpdateAuthorizationStep(
    SamService samService, BillingProfile profile, AuthenticatedUserRequest userRequest)
    implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    samService.verifyAuthorization(
        userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // No side effects to undo
    return StepResult.getStepResultSuccess();
  }
}
