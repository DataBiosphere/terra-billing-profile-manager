package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DeleteProfilePoliciesStep(TpsApiDispatch tpsApiDispatch, UUID profileId)
    implements Step {

  private static final Logger logger = LoggerFactory.getLogger(DeleteProfilePoliciesStep.class);

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    tpsApiDispatch.deletePao(profileId);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // We can't un-delete the PAO, so just surface the error that caused the flight to fail.
    logger.error("Unable to undo deletion of policies on profile {} ", profileId);
    return context.getResult();
  }
}
