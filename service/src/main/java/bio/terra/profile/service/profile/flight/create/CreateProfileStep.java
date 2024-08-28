package bio.terra.profile.service.profile.flight.create;

import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Step to create a billing profile in the database. */
record CreateProfileStep(ProfileDao profileDao, BillingProfile profile, String initiatingUser)
    implements Step {
  private static final Logger logger = LoggerFactory.getLogger(CreateProfileStep.class);

  @Override
  public StepResult doStep(FlightContext flightContext) {
    var createdProfile = profileDao.createBillingProfile(profile, initiatingUser);
    logger.info("Profile created with id {}", createdProfile.id());

    FlightMap workingMap = flightContext.getWorkingMap();
    workingMap.put(ProfileMapKeys.PROFILE, createdProfile);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) {
    profileDao.deleteBillingProfileById(profile.id());
    return StepResult.getStepResultSuccess();
  }
}
