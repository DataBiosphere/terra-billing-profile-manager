package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.azure.AzureService;
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
    AzureService azureService = appContext.getBean(AzureService.class);
    ApplicationService appService = appContext.getBean(ApplicationService.class);
    AzureConfiguration azureConfig = appContext.getBean(AzureConfiguration.class);

    BillingProfile profile =
        inputParameters.get(JobMapKeys.REQUEST.getKeyName(), BillingProfile.class);
    AuthenticatedUserRequest user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    MetricUtils.recordProfileCreation(
        () -> {
          addStep(new GetProfileStep(profileDao, profile));

          addStep(new CreateProfileStep(profileDao, profile, user));
          switch (profile.cloudPlatform()) {
            case GCP -> addStep(new CreateProfileVerifyAccountStep(crlService, profile, user));
            case AZURE -> addStep(
                new CreateProfileVerifyDeployedApplicationStep(
                    azureService, profile, azureConfig, user));
          }
          addStep(new CreateProfileAuthzIamStep(samService, profile, user));

          if (CloudPlatform.AZURE == profile.cloudPlatform()) {
            // we can link the profile to the MRG only after the Sam resource has been created
            addStep(new LinkBillingProfileIdToMrgStep(appService, samService, profile, user));
          }
        },
        profile.cloudPlatform());
  }
}
