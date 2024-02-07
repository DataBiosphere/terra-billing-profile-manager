package bio.terra.profile.service.profile.flight.update;

import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import javax.annotation.Nullable;
import org.springframework.http.HttpStatus;

public record UpdateProfileStep(
    ProfileDao profileDao,
    BillingProfile profile,
    @Nullable String description,
    @Nullable String billingAccountId)
    implements Step {

  @Override
  public StepResult doStep(FlightContext flightContext) {
    var workingMap = flightContext.getWorkingMap();

    if (profileDao.updateProfile(profile.id(), description, billingAccountId)) {
      var updatedProfile = profileDao.getBillingProfileById(profile.id());
      workingMap.put(JobMapKeys.RESPONSE.getKeyName(), updatedProfile);
      workingMap.put(JobMapKeys.STATUS_CODE.getKeyName(), HttpStatus.OK);
      return StepResult.getStepResultSuccess();
    } else {
      return new StepResult(
          StepStatus.STEP_RESULT_FAILURE_FATAL,
          new ProfileNotFoundException(
              String.format("Profile %s not found, update failed.", profile.id())));
    }
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) {
    // restore billing profile to pre-flight state
    profileDao.updateProfile(
        profile.id(), profile.description(), profile.getRequiredBillingAccountId());
    return StepResult.getStepResultSuccess();
  }
}
