package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.generated.model.ApiCloudPlatform;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import java.util.UUID;
import org.springframework.context.ApplicationContext;

public class DeleteProfileFlight extends Flight {

  public DeleteProfileFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    ProfileDao profileDao = appContext.getBean(ProfileDao.class);
    CrlService crlService = appContext.getBean(CrlService.class);
    SamService samService = appContext.getBean(SamService.class);

    var profileId = inputParameters.get(ProfileMapKeys.PROFILE_ID, UUID.class);
    var platform =
        inputParameters.get(JobMapKeys.CLOUD_PLATFORM.getKeyName(), ApiCloudPlatform.class);
    var user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    // TODO what is the correct logic when a profile is deleted if it is being used by
    // workspaces/datasets?

    addStep(new DeleteProfileStep(profileDao, profileId));
    addStep(new DeleteProfileAuthzIamStep(samService, profileId, user));
  }
}
