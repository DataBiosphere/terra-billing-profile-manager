package bio.terra.profile.service.profile.flight.delete;

import bio.terra.profile.db.ProfileDao;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import java.util.UUID;

/** Step to delete a billing profile from the database. */
record DeleteProfileStep(ProfileDao profileDao, UUID profileId) implements Step {
  @Override
  public StepResult doStep(FlightContext context) {
    profileDao.deleteBillingProfileById(profileId);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }
}
