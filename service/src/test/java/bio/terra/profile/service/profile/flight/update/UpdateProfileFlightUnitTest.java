package bio.terra.profile.service.profile.flight.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.common.VerifyUserBillingAccountAccessStep;
import bio.terra.stairway.FlightMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class UpdateProfileFlightUnitTest extends BaseUnitTest {

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  ApplicationContext setUpMockContext(List<Object> beans) {
    var context = mock(ApplicationContext.class);
    beans.forEach(bean -> doReturn(bean).when(context).getBean(bean.getClass()));
    return context;
  }

  @Test
  void updateProfileDescriptionSteps() {
    var profileDao = mock(ProfileDao.class);
    var crlService = mock(GcpCrlService.class);
    var samService = mock(SamService.class);
    var azureService = mock(AzureService.class);
    var azureConfig = mock(AzureConfiguration.class);

    var context =
        setUpMockContext(List.of(profileDao, crlService, samService, azureService, azureConfig));

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var request = new UpdateProfileRequest().description("description");

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);
    inputParameters.put(ProfileMapKeys.PROFILE, profile);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(3, steps.size());
    assertEquals(VerifyProfileMetadataUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(1).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(2).getClass());
  }

  @Test
  void updateProfileBillingAccountSteps() {
    var profileDao = mock(ProfileDao.class);
    var crlService = mock(GcpCrlService.class);
    var samService = mock(SamService.class);
    var azureService = mock(AzureService.class);
    var azureConfig = mock(AzureConfiguration.class);

    var context =
        setUpMockContext(List.of(profileDao, crlService, samService, azureService, azureConfig));

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var request = new UpdateProfileRequest().billingAccountId("billingAccount");

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);
    inputParameters.put(ProfileMapKeys.PROFILE, profile);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(4, steps.size());
    assertEquals(VerifyAccountUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(VerifyUserBillingAccountAccessStep.class, steps.get(1).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(2).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(3).getClass());
  }

  @Test
  void updateProfileDescriptionAndBillingAccountSteps() {
    var profileDao = mock(ProfileDao.class);
    var crlService = mock(GcpCrlService.class);
    var samService = mock(SamService.class);
    var azureService = mock(AzureService.class);
    var azureConfig = mock(AzureConfiguration.class);

    var context =
        setUpMockContext(List.of(profileDao, crlService, samService, azureService, azureConfig));

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var request =
        new UpdateProfileRequest().description("description").billingAccountId("billingAccount");

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);
    inputParameters.put(ProfileMapKeys.PROFILE, profile);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(5, steps.size());
    assertEquals(VerifyProfileMetadataUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(VerifyAccountUpdateAuthorizationStep.class, steps.get(1).getClass());
    assertEquals(VerifyUserBillingAccountAccessStep.class, steps.get(2).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(3).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(4).getClass());
  }
}
