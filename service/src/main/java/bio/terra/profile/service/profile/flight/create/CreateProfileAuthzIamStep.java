package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;

/** Step to create a profile resource in Sam. */
record CreateProfileAuthzIamStep(
    SamService samService, BillingProfile profile, AuthenticatedUserRequest user) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    samService.createProfileResource(user, profile.id());
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    samService.deleteProfileResource(user, profile.id());
    return StepResult.getStepResultSuccess();
  }
}
