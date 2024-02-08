package bio.terra.profile.service.profile.flight.update;

import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import org.springframework.http.HttpStatus;

public record UpdateProfileSetResponseStep(ProfileDao profileDao, BillingProfile profile)
    implements Step {

  @Override
  public StepResult doStep(FlightContext flightContext) {
    var workingMap = flightContext.getWorkingMap();

    var updatedProfile = profileDao.getBillingProfileById(profile.id());
    workingMap.put(JobMapKeys.RESPONSE.getKeyName(), updatedProfile);
    workingMap.put(JobMapKeys.STATUS_CODE.getKeyName(), HttpStatus.OK);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) {
    // Nothing to undo
    return StepResult.getStepResultSuccess();
  }
}
