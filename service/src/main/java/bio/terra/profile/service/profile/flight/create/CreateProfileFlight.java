package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import org.springframework.context.ApplicationContext;

public class CreateProfileFlight extends Flight {

  public CreateProfileFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    ProfileDao profileDao = appContext.getBean(ProfileDao.class);
    CrlService crlService = appContext.getBean(CrlService.class);
    SamService samService = appContext.getBean(SamService.class);

    BillingProfile profile =
        inputParameters.get(JobMapKeys.REQUEST.getKeyName(), BillingProfile.class);
    AuthenticatedUserRequest user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    addStep(new GetProfileStep(profileDao, profile));
    addStep(new CreateProfileStep(profileDao, profile, user));
    switch (profile.cloudPlatform()) {
      case GCP:
        addStep(new CreateProfileVerifyAccountStep(crlService, profile, user));
        break;
      case AZURE:
        addStep(new CreateProfileVerifyDeployedApplicationStep(crlService, profile, user));
        break;
    }
    addStep(new CreateProfileAuthzIamStep(samService, profile, user));
  }
}
