package bio.terra.profile.service.profile.flight.create;

import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public record RecordProfileCreationStep(ProfileChangeLogDao changeLogDao) implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightMap workingMap = context.getWorkingMap();
    BillingProfile createdProfile = workingMap.get(ProfileMapKeys.PROFILE, BillingProfile.class);
    if (createdProfile == null) {
      throw new MissingRequiredFieldsException(
          "Missing required flight map key: " + ProfileMapKeys.PROFILE);
    }

    changeLogDao.recordProfileCreate(createdProfile);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
