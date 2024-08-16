package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.gcp.GcpService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import org.springframework.context.ApplicationContext;

public class CreateProfileFlight extends Flight {

  public CreateProfileFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    ProfileDao profileDao = appContext.getBean(ProfileDao.class);
    ProfileChangeLogDao changeLogDao = appContext.getBean(ProfileChangeLogDao.class);
    GcpService gcpService = appContext.getBean(GcpService.class);
    SamService samService = appContext.getBean(SamService.class);
    AzureService azureService = appContext.getBean(AzureService.class);
    AzureConfiguration azureConfig = appContext.getBean(AzureConfiguration.class);
    TpsApiDispatch tpsApiDispatch = appContext.getBean(TpsApiDispatch.class);

    ProfileDescription profileDescription =
        inputParameters.get(JobMapKeys.REQUEST.getKeyName(), ProfileDescription.class);
    BillingProfile billingProfile = profileDescription.billingProfile();

    AuthenticatedUserRequest user =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    addStep(new GetProfileStep(profileDao, billingProfile));
    addStep(new CreateProfileStep(profileDao, billingProfile, user));
    switch (billingProfile.cloudPlatform()) {
      case GCP:
        addStep(
            new CreateProfileVerifyAccountStep(
                gcpService, billingProfile.billingAccountId(), user));
        break;
      case AZURE:
        addStep(
            new CreateProfileVerifyDeployedApplicationStep(
                azureService, billingProfile, azureConfig, user));
        break;
    }
    addStep(new CreateProfileAuthzIamStep(samService, billingProfile, user));
    addStep(new CreateProfilePoliciesStep(tpsApiDispatch, profileDescription, user));

    if (CloudPlatform.AZURE == billingProfile.cloudPlatform()) {
      // we can link the profile to the MRG only after the Sam resource has been created
      addStep(new LinkBillingProfileIdToMrgStep(samService, billingProfile, user));
    }
    var initiatingUser = inputParameters.get(JobMapKeys.INITIATING_USER.getKeyName(), String.class);
    addStep(new RecordProfileCreationStep(changeLogDao, initiatingUser));
    addStep(new CreateProfileFinishStep(changeLogDao));
  }
}
