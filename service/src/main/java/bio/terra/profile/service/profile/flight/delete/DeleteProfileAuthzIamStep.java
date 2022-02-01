package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.iam.SamService;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import java.util.UUID;

/** Step to delete a billing profile resource in Sam. */
record DeleteProfileAuthzIamStep(
    SamService samService, UUID profileId, AuthenticatedUserRequest user) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    samService.deleteProfileResource(user, profileId);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }
}
