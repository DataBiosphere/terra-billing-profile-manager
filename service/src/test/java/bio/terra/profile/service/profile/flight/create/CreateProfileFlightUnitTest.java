package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.stairway.FlightMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class CreateProfileFlightUnitTest extends BaseUnitTest {

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
  void createGcpProfileSteps() {
    var profileDao = mock(ProfileDao.class);
    var crlService = mock(GcpCrlService.class);
    var samService = mock(SamService.class);
    var azureService = mock(AzureService.class);
    var azureConfig = mock(AzureConfiguration.class);

    var context =
        setUpMockContext(
            List.of(
                profileDao,
                crlService,
                samService,
                azureService,
                azureConfig,
                mock(ProfileChangeLogDao.class)));

    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234");

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), profile);

    var flight = new CreateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(7, steps.size());
    assertEquals(GetProfileStep.class, steps.get(0).getClass());
    assertEquals(steps.get(1).getClass(), CreateProfileStep.class);
    assertEquals(steps.get(2).getClass(), CreateProfileVerifyAccountStep.class);
    assertEquals(steps.get(3).getClass(), CreateProfileAuthzIamStep.class);
    assertEquals(steps.get(4).getClass(), CreateProfilePoliciesStep.class);
    assertEquals(steps.get(5).getClass(), RecordProfileCreationStep.class);
    assertEquals(steps.get(6).getClass(), CreateProfileFinishStep.class);
  }

  @Test
  void createAzureProfileSteps() {
    var profileDao = mock(ProfileDao.class);
    var crlService = mock(GcpCrlService.class);
    var samService = mock(SamService.class);
    var azureService = mock(AzureService.class);
    var azureConfig = mock(AzureConfiguration.class);

    var context =
        setUpMockContext(
            List.of(
                profileDao,
                crlService,
                samService,
                azureService,
                azureConfig,
                mock(ProfileChangeLogDao.class)));

    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "ABCD1234";
    var profile = ProfileFixtures.createAzureBillingProfileDescription(tenantId, subId, mrgId);

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), profile);

    var flight = new CreateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(8, steps.size());
    assertEquals(steps.get(0).getClass(), GetProfileStep.class);
    assertEquals(steps.get(1).getClass(), CreateProfileStep.class);
    assertEquals(steps.get(2).getClass(), CreateProfileVerifyDeployedApplicationStep.class);
    assertEquals(steps.get(3).getClass(), CreateProfileAuthzIamStep.class);
    assertEquals(steps.get(4).getClass(), CreateProfilePoliciesStep.class);
    assertEquals(steps.get(5).getClass(), LinkBillingProfileIdToMrgStep.class);
    assertEquals(steps.get(6).getClass(), RecordProfileCreationStep.class);
    assertEquals(steps.get(7).getClass(), CreateProfileFinishStep.class);
  }
}
