package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

import java.util.UUID;

public record RecordProfileDeleteStep(ProfileChangeLogDao changeLogDao, UUID profileId, AuthenticatedUserRequest userRequest) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    changeLogDao.recordProfileDelete(profileId, userRequest.getEmail());
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
