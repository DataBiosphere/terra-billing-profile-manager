package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import org.springframework.context.ApplicationContext;

public class DeleteProfileFlight extends Flight {

  public DeleteProfileFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    ProfileDao profileDao = appContext.getBean(ProfileDao.class);
    SamService samService = appContext.getBean(SamService.class);

    var profile = inputParameters.get(ProfileMapKeys.PROFILE, BillingProfile.class);
    var profileId = profile.id();
    var platform = inputParameters.get(JobMapKeys.CLOUD_PLATFORM.getKeyName(), CloudPlatform.class);
    var user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    // we expect upstream creators of billing profiles to manage references to billing profiles and
    // only delete when
    // there are no more entities dependent on this profile
    addStep(new DeleteProfileStep(profileDao, profileId));
    if (CloudPlatform.AZURE == platform) {
      addStep(new UnlinkBillingProfileIdFromMrgStep(samService, profile, user));
    }
    addStep(new DeleteProfileAuthzIamStep(samService, profileId, user));
    MetricUtils.incrementProfileDeletion(platform);
  }
}
