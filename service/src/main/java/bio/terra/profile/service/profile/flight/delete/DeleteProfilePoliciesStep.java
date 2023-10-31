package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import java.util.UUID;

public record DeleteProfilePoliciesStep(TpsApiDispatch tpsApiDispatch, UUID profileId)
    implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    tpsApiDispatch.deletePao(profileId);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // todo: re-create policies?
    return StepResult.getStepResultSuccess();
  }
}
