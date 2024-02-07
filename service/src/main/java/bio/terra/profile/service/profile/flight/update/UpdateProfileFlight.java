package bio.terra.profile.service.profile.flight.update;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.common.VerifyAccountUserAccessStep;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import org.springframework.context.ApplicationContext;

public class UpdateProfileFlight extends Flight {

  public UpdateProfileFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    ProfileDao profileDao = appContext.getBean(ProfileDao.class);
    GcpCrlService gcpCrlService = appContext.getBean(GcpCrlService.class);
    SamService samService = appContext.getBean(SamService.class);

    UpdateProfileRequest updateProfileRequest =
        inputParameters.get(JobMapKeys.REQUEST.getKeyName(), UpdateProfileRequest.class);
    BillingProfile profile = inputParameters.get(ProfileMapKeys.PROFILE, BillingProfile.class);

    AuthenticatedUserRequest user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    if (updateProfileRequest.getDescription() != null) {
      addStep(new VerifyProfileMetadataUpdateAuthorizationStep(samService, profile, user));
    }

    if (updateProfileRequest.getBillingAccountId() != null) {
      addStep(new VerifyAccountUpdateAuthorizationStep(samService, profile, user));
      addStep(new VerifyAccountUserAccessStep(gcpCrlService, profile, user));
    }

    addStep(
        new UpdateProfileStep(
            profileDao,
            profile,
            updateProfileRequest.getDescription(),
            updateProfileRequest.getBillingAccountId()));
  }
}
