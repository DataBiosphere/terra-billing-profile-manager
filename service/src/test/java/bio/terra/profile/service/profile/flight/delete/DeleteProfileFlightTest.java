package bio.terra.profile.service.profile.flight.delete;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.ProfileService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class DeleteProfileFlightTest extends BaseSpringUnitTest {

  @Autowired ProfileService profileService;
  @Autowired AzureConfiguration azureConfiguration;
  @MockBean CrlService crlService;

  @MockBean ProfileDao profileDao;
  @MockBean SamService samService;
  @MockBean AzureService azureService;
  @MockBean ApplicationService applicationService;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @Test
  void deletingAzureProfileUnlinksMRG() throws InterruptedException {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subscriptionId, mrgId);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest);
    verify(samService).deleteManagedResourceGroup(profile.id(), userRequest);
  }

  @Test
  void deletingGCPProfileDoesNotDoUnlinkMRGStep() throws InterruptedException {
    var profile = ProfileFixtures.createGcpBillingProfile("fake-gcp-id");
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest);
    verify(samService, never()).deleteManagedResourceGroup(any(), any());
  }
}
