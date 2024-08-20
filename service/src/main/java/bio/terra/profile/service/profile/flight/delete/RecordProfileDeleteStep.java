package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import java.util.UUID;

public record RecordProfileDeleteStep(
    ProfileChangeLogDao changeLogDao, UUID profileId, String initiatingUser) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    changeLogDao.recordProfileDelete(profileId, initiatingUser);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
